package com.xxl.job.workbench.config;

import com.google.common.collect.Lists;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableAsync
public class ThreadPoolConfiguration implements ApplicationContextAware {


    @Value("${threadpool.corePoolSize:16}")
    private int corePoolSize;
    @Value("${threadpool.maxPoolSize:100}")
    private int maxPoolSize;
    @Value("${threadpool.queueCapacity:200}")
    private int queueCapacity;
    // 最大线程的存活时间
    @Value("${threadpool.keepAliveSeconds:5}")
    private int keepAliveSeconds;
    // shutdownNow的等待时间
    @Value("${threadpool.awaitTerminationSeconds:30}")
    private int awaitTerminationSeconds;

    private static final String THREAD_NAME_PREFIX = "AsyncExecutorThread-";

    private static ApplicationContext applicationContext;

    /**线程池name*/
    public static final String WORK_FOR_DB_LOG = "th-db_log";

    @Bean("asyncExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = getThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("com.dmall.demeter.trade-thread-" + executor);
        executor.initialize();
        return executor;
    }

    private ThreadPoolTaskExecutor getThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        // CALLER_RUNS：不在新线程中执行任务，而是有调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (ThreadPoolConfiguration.applicationContext == null) {
            ThreadPoolConfiguration.applicationContext = applicationContext;
            setBean();
        }
    }

    private void setBean() {
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext
                .getAutowireCapableBeanFactory();
        List<String> list = Lists.newArrayList();
        list.add(WORK_FOR_DB_LOG);
        list.forEach(beanName -> {
            beanFactory.registerSingleton(beanName, asyncExecutorForAutoReviewTask(beanName));
        });
    }

    private Executor asyncExecutorForAutoReviewTask(String threadName) {
        ThreadPoolTaskExecutor executor = getThreadPoolTaskExecutor();
        executor.setThreadFactory(new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, threadName+ "-"+
                        threadNumber.incrementAndGet());
            }
        });
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }
}
