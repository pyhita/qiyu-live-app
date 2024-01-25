package com.yangtao.config;

import com.alibaba.fastjson2.JSON;
import com.yangtao.dao.po.UserPO;
import com.yangtao.dto.UserDTO;
import com.yangtao.framework.redis.key.UserProviderCacheKeyBuilder;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@Configuration
public class RocketMQConsumerConfig implements InitializingBean {

    private static Logger LOGGER = LoggerFactory.getLogger(RocketMQConsumerConfig.class);

    @Autowired
    private RocketMQConsumerProperties consumerProperties;
    @Resource
    private RedisTemplate<String, UserPO> redisTemplate;
    @Autowired
    private UserProviderCacheKeyBuilder keyBuilder;

    // 构造方法，属性注入之后 执行
    @Override
    public void afterPropertiesSet() throws Exception {
        initRocketMQConsumer();
    }

    private void initRocketMQConsumer() {
        // 设置一些必要的参数
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
        consumer.setNamesrvAddr(consumerProperties.getNameSrv());
        consumer.setConsumerGroup(consumerProperties.getGroupName());
        // 设置每次消息消费的数量
        consumer.setConsumeMessageBatchMaxSize(1);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        // 设置订阅的topic
        try {
            consumer.subscribe("user-update-cache", "*");
            consumer.setMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                    // 从消息体中 拿到相应的数据
                    String body = new String(list.get(0).getBody());
                    UserDTO userDTO = JSON.parseObject(body, UserDTO.class);
                    if (userDTO == null || userDTO.getUserId() == null) {
                        LOGGER.error("user id为空，发生异常, 消息体为：{}", body);
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    }

                    // 进行延迟双删
                    String userInfoKey = keyBuilder.buildUserInfoKey(userDTO.getUserId());
                    redisTemplate.delete(userInfoKey);
                    LOGGER.error("删除双删，userDTO is {}", userDTO);

                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });
            consumer.start();
            LOGGER.info("mq消费者启动成功,nameSrv is {}", consumerProperties.getNameSrv());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
