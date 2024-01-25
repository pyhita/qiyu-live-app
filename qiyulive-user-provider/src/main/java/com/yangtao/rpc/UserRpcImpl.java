package com.yangtao.rpc;

import com.yangtao.dto.UserDTO;
import com.yangtao.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;
import java.util.Map;

@DubboService
@RequiredArgsConstructor
public class UserRpcImpl implements IUserRpc {

    private final IUserService userService;

    @Override
    public UserDTO getByUserId(Long userId) {
        return userService.getUserById(userId);
    }

    @Override
    public Boolean updateById(UserDTO userDTO) {
        return userService.updateById(userDTO);
    }

    @Override
    public Map<Long, UserDTO> getUserByIdList(List<Long> userIds) {
        return userService.getUserByIdList(userIds);
    }

}
