package com.yangtao.rpc;

import com.yangtao.dto.UserDTO;

import java.util.List;
import java.util.Map;

public interface IUserRpc {

    UserDTO getByUserId(Long userId);

    Boolean updateById(UserDTO userDTO);

    Map<Long, UserDTO> getUserByIdList(List<Long> userIds);

}
