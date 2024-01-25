package com.yangtao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "qiyu.rmq.consumer")
@Configuration
@Data
public class RocketMQConsumerProperties {
    //rocketmq的nameSever地址
    private String nameSrv;
    //分组名称
    private String groupName;
}
