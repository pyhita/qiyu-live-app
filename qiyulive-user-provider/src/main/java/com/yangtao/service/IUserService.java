package com.yangtao.service;

import com.yangtao.dto.UserDTO;

import java.util.List;
import java.util.Map;

public interface IUserService {

    UserDTO getUserById(Long userId);

    Boolean updateById(UserDTO userDTO);

    Map<Long, UserDTO> getUserByIdList(List<Long> userIds);

}
