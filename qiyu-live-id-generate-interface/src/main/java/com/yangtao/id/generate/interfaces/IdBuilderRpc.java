package com.yangtao.id.generate.interfaces;

public interface IdBuilderRpc {

    /**
     * 根据本地步长度来生成唯一id(区间性递增)
     *
     * @return
     */
    Long increaseSeqId(int code);

    /**
     * 生成的是非连续性id
     *
     * @param code
     * @return
     */
    Long increaseUnSeqId(int code);

    /**
     * 根据本地步长度来生成唯一id(区间性递增)
     *
     * @param code
     * @return
     */
    String increaseSeqStrId(int code);
}