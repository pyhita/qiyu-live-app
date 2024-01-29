package com.yangtao.id.generate.provider.service.bo;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@Data
public class LocalSeqIdBO {

    //mysql配置的id
    private Integer id;
    //对应分布式id的配置说明
    private String desc;
    //当前在本地内存的id值
    private AtomicLong currentValue;
    //本地内存记录id段的开始位置
    private Long currentStart;
    //本地内存记录id段的结束位置
    private Long nextThreshold;
    //步长
    private Integer step;

}
