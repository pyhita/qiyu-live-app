package com.yangtao.id.generate.provider.service;

public interface IdBuilderService {

    /**
     * 根据本地步长度来生成唯一id(区间性递增)
     *
     * @return
     */
    Long increaseSeqId(Integer code);

    /**
     * 生成的是非连续性id
     *
     * @param code
     * @return
     */
    Long increaseUnSeqId(Integer code);

    /**
     * 根据本地步长度来生成唯一id(区间性递增)
     *
     * @param code
     * @return
     */
    String increaseSeqStrId(Integer code);

}
