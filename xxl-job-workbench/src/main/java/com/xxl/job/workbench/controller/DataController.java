package com.xxl.job.workbench.controller;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.workbench.dao.OrderEntityMapper;
import com.xxl.job.workbench.property.Area;
import com.xxl.job.workbench.property.Human;
import com.xxl.job.workbench.property.PeopleArea;
import org.mybatis.spring.batch.MyBatisBatchItemWriter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class DataController {

    @Autowired
    private OrderEntityMapper mapper;
    // 1、百万级Excel插入怎么处理
    // 2、高并发下写数据，如何快速响应  线程池非常容易打满
    // 3、大批量消费mq，如何提升数据处理效率
    @PostMapping("/saveBatch")
    public ReturnT<String> save(){
        // 如何做join
        List<Area> area = new ArrayList<>();
        area.add(new Area("1", "jiangsu"));
        area.add(new Area("2", "shandong"));
        area.add(new Area("3", "anhui"));
        List<Human> people = new ArrayList<>();
        people.add(new Human("1", "xiaoming", "001"));
        people.add(new Human("2", "xiaohong", "002"));
        people.add(new Human("3","zhang", "003"));
        people.add(new Human("1", "wang", "004"));
        people.add(new Human("2", "qian", "005"));
        Map<String, Area> map = area.stream().collect(Collectors.toMap(Area::getId, r->r));
        List<PeopleArea> res = people.stream().map(r -> {
            PeopleArea t = new PeopleArea();
            try {
                BeanUtils.copyProperties(r, t);
                BeanUtils.copyProperties(map.get(r.getId()), t);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return t;
        }).collect(Collectors.toList());
        System.out.println(res);
        // 如何做group by求总数，最大值
        Map<String, Long> collect = area.stream().collect(Collectors.groupingBy(Area::getId, Collectors.counting()));
        System.out.println(collect);
        Collection<Human> collect1 = people.stream().collect(Collectors.groupingBy(Human::getId,
                Collectors.maxBy(Comparator.comparing(Human::getName)))).values().
                stream().map(r->r.get()).collect(Collectors.toList());
        System.out.println(collect1);
        return ReturnT.SUCCESS;
    }

}
