package com.yangtao.config;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class RocketMQProducerConfig {

    private final static Logger LOGGER = LoggerFactory.getLogger(RocketMQProducerConfig.class);

    @Autowired
    private RocketMQProducerProperties producerProperties;
    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public MQProducer mqProducer() {
        ThreadPoolExecutor aysncExecutor = new ThreadPoolExecutor(100,
                150,
                3,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName(applicationName + ":rockermq-producer-" + ThreadLocalRandom.current().nextInt(1000));
                        return thread;
                    }
                }
        );

        // 设置必要的参数
        DefaultMQProducer mqProducer = new DefaultMQProducer();

        try {
            mqProducer.setNamesrvAddr(producerProperties.getNameSrv());
            mqProducer.setProducerGroup(producerProperties.getGroupName());
            mqProducer.setRetryTimesWhenSendFailed(producerProperties.getRetryTimes());
            mqProducer.setRetryTimesWhenSendAsyncFailed(producerProperties.getRetryTimes());
            mqProducer.setRetryAnotherBrokerWhenNotStoreOK(true);
            mqProducer.setSendMsgTimeout(producerProperties.getSendTimeOut());
            mqProducer.setAsyncSenderExecutor(aysncExecutor);
            mqProducer.start();

            LOGGER.info("mq生产者启动成功,nameSrv is {}",  producerProperties.getNameSrv());
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }

        return mqProducer;
    }
}
