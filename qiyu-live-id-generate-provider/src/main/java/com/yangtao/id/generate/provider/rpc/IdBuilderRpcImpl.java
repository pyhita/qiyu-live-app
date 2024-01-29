package com.yangtao.id.generate.provider.rpc;

import com.yangtao.id.generate.interfaces.IdBuilderRpc;
import com.yangtao.id.generate.provider.service.IdBuilderService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class IdBuilderRpcImpl implements IdBuilderRpc {

    @Resource
    private IdBuilderService idBuilderService;

    @Override
    public Long increaseSeqId(int code) {
        return idBuilderService.increaseSeqId(code);
    }

    @Override
    public Long increaseUnSeqId(int code) {
        return idBuilderService.increaseUnSeqId(code);
    }

    @Override
    public String increaseSeqStrId(int code) {
        return idBuilderService.increaseSeqStrId(code);
    }
}
