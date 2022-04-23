package com.xxl.job.workbench.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xxl.job.core.biz.model.CategoryEntity;
import com.xxl.job.core.biz.model.Catelog2Vo;
import com.xxl.job.core.util.StringUtils;
import com.xxl.job.workbench.dao.CategoryMapper;
import com.xxl.job.workbench.service.CategoryService;
import net.bytebuddy.asm.Advice;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redisson;
    @Resource
    private CategoryMapper categoryMapper;

    @Override
    public List<CategoryEntity> listWithTree() {


        //1、查询出所有分类
        List<CategoryEntity> entities = categoryMapper.selectList();

        //2、组装成父子的树形结构

        //2.1)、找到所有一级分类
        List<CategoryEntity> levelMenus = entities.stream()
                .filter(e -> e.getParentCid() == 0)
                .map((menu) -> {
                    menu.setChildren(getChildrens(menu, entities));
                    return menu;
                })
                .sorted((menu, menu2) -> {
                    return (menu.getSort() == null ?
                            0 : menu.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                })
                .collect(Collectors.toList());

        return levelMenus;
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid().equals(root.getCatId());
        }).map(categoryEntity -> {
            //1、找到子菜单(递归)
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu, menu2) -> {
            //2、菜单的排序
            return (menu.getSort() == null ?
                    0 : menu.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;

    }

    @Override
    public String testRedis(){
        ValueOperations ops = redisTemplate.opsForValue();
        UUID uuid = UUID.randomUUID();
        String key = uuid.toString();
        ops.set(key,"4", 30, TimeUnit.MINUTES);
        String value = StringUtils.toString(ops.get(key));
        return value;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {
        synchronized (this){
            return getCatalogJsonFromDb();
        }
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {
        ValueOperations<String,String> ops = redisTemplate.opsForValue();
        // 这里注意，需要先查一下缓存，看看是否存在数据
        String catalogJson = ops.get("catalogJson");
        // 结果不为空，证明缓存中有，直接返回数据
        if(!org.springframework.util.StringUtils.isEmpty(catalogJson)){
            return JSON.parseObject(catalogJson.toString(), new TypeReference<Map<String, List<Catelog2Vo>>>(){});
        }
        System.out.println("查询了数据库");

        //将数据库的多次查询变为一次
        List<CategoryEntity> selectList = categoryMapper.selectList();

        //1、查出所有分类
        //1、1）查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //封装数据
        Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream()
                .collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //1、每一个的一级分类,查到这个一级分类的二级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());

                    //2、封装上面的结果
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        catelog2Vos = categoryEntities.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null,
                                    l2.getCatId().toString(), l2.getName().toString());

                            //1、找当前二级分类的三级分类封装成vo
                            List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());

                            if (level3Catelog != null) {
                                List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                                    //2、封装成指定格式
                                    Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(
                                            l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                                    return category3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(category3Vos);
                            }

                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }

                    return catelog2Vos;
                }));
        redisTemplate.opsForValue().set("catalogJson", JSON.toJSONString(parentCid), 30, TimeUnit.MINUTES);
        return parentCid;
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonWithRedisLock() {
        ValueOperations<String,String> ops = redisTemplate.opsForValue();
        String uuid = UUID.randomUUID().toString();
        // 这里必须是原子操作
        Boolean success = ops.setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        if(success){
            System.out.println("获取分布式锁成功");
            // 加锁成功，直接查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = new HashMap<>();
            try{
                catalogJsonFromDb = getCatalogJsonFromDb();
            }finally {
                // 判断下是不是自己加的锁，否则过期了就会导致删除别的线程加的锁
                // 解锁也要保证原子性
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return " +
                        "redis.call('del', KEYS[1]) else return 0 end";
                Object res = redisTemplate.execute(new DefaultRedisScript<Integer>(script, Integer.class),
                        Arrays.asList("lock"), uuid);
            }
            return catalogJsonFromDb;
        }else{
            System.out.println("获取分布式锁失败");
            // 加锁失败，要重试;这里是采用自旋的方式
            return getCatalogJsonWithRedisLock();
        }
    }

    public Map<String, List<Catelog2Vo>> getCatalogJsonWithRedissonLock() {
        ValueOperations<String,String> ops = redisTemplate.opsForValue();
        String uuid = UUID.randomUUID().toString();
        // 这里必须是原子操作
        RLock catalogLock = redisson.getLock("catalogLock");
        Boolean success = ops.setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        if(success){
            System.out.println("获取分布式锁成功");
            // 加锁成功，直接查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = new HashMap<>();
            try{
                catalogJsonFromDb = getCatalogJsonFromDb();
            }finally {
                // 判断下是不是自己加的锁，否则过期了就会导致删除别的线程加的锁
                // 解锁也要保证原子性
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return " +
                        "redis.call('del', KEYS[1]) else return 0 end";
                Object res = redisTemplate.execute(new DefaultRedisScript<Integer>(script, Integer.class),
                        Arrays.asList("lock"), uuid);
            }
            return catalogJsonFromDb;
        }else{
            System.out.println("获取分布式锁失败");
            // 加锁失败，要重试;这里是采用自旋的方式
            return getCatalogJsonWithRedisLock();
        }
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parentCid) {
        List<CategoryEntity> categoryEntities = selectList.stream()
                .filter(item -> item.getParentCid().equals(parentCid))
                .collect(Collectors.toList());
        return categoryEntities;
        // return this.baseMapper.selectList(
        //         new QueryWrapper<CategoryEntity>().eq("parent_cid", parentCid));
    }

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        // 直接放JSON，兼容性比较好
        ValueOperations<String,String> ops = redisTemplate.opsForValue();
        String catalogJson = ops.get("catalogJson");
        if(org.springframework.util.StringUtils.isEmpty(catalogJson)){
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonWithRedissonLock();
            return catalogJsonFromDb;
        }
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>(){});
    }


}
