package com.yangtao.id.generate.provider.dao.po;

import lombok.Data;

import java.util.Date;

@Data
public class IdBuilderPO {

    private Integer id;

    /**
     * id备注描述
     */
    private String remark;

    /**
     * 初始化值
     */
    private long initNum;

    private int step;

    /**
     * 是否是有序的id
     */
    private int isSeq;

    /**
     * 当前id所在阶段的开始值
     */
    private long currentStart;

    /**
     * 当前id所在阶段的阈值
     */
    private long nextThreshold;

    /**
     * 业务代码前缀
     */
    private String idPrefix;

    /**
     * 乐观锁版本号
     */
    private int version;

    private Date createTime;

    private Date updateTime;
}
