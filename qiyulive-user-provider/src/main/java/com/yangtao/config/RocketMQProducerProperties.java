package com.yangtao.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "qiyu.rmp.producer")
@Configuration
@Data
public class RocketMQProducerProperties {
    //rocketmq的nameSever地址
    private String nameSrv = "192.168.233.8:9876";
    //分组名称
    private String groupName = "qiyu-live-user";
    //消息重发次数
    private int retryTimes = 3;
    //发送超时时间
    private int sendTimeOut = 3000;
}
