package com.yangtao;

import com.yangtao.config.RocketMQProducerProperties;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableDubbo(scanBasePackages = "com.yangtao.rpc")
@MapperScan(basePackages = {"com.yangtao.dao.mapper"})
@EnableDiscoveryClient
public class UserProviderApplication {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(UserProviderApplication.class);
        ConfigurableApplicationContext ctx = springApplication.run(args);
        RocketMQProducerProperties mqProducerProperties = ctx.getBean(RocketMQProducerProperties.class);
        System.out.println("mqProducerProperties = " + mqProducerProperties);
    }
}