package com.yangtao.id.generate.provider;

import com.yangtao.id.generate.provider.dao.mapper.IdBuilderMapper;
import com.yangtao.id.generate.provider.dao.po.IdBuilderPO;
import com.yangtao.id.generate.provider.service.IdBuilderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
@EnableDiscoveryClient
public class IdProviderApplication {

    @Autowired
    private IdBuilderService idBuilderService;

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(IdProviderApplication.class, args);
        IdBuilderMapper mapper = ctx.getBean(IdBuilderMapper.class);
        List<IdBuilderPO> idBuilderPOS = mapper.selectAll();
        System.out.println("idBuilderPOS = " + idBuilderPOS);
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            for (int i = 0; i < 1300; i++) {
                System.out.println(idBuilderService.increaseUnSeqId(1));
            }
        };
    }

}
