package com.yangtao.controller;

import com.yangtao.dto.UserDTO;
import com.yangtao.request.UserListRequest;
import com.yangtao.rpc.IUserRpc;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    //    @DubboReference(url = "dubbo://192.168.0.109:20880/com.yangtao.rpc.UserRpcImpl")
    @DubboReference
    private IUserRpc iUserRpc;

    @GetMapping("/getByUserId")
    public UserDTO getByUserId(Long userId) {

        return iUserRpc.getByUserId(userId);
    }

    @GetMapping("/getByUserIdList")
    public Map<Long, UserDTO> getByUserId(UserListRequest request) {

        return iUserRpc.getUserByIdList(request.getUserIdList());
    }

    @PutMapping("/updateById")
    public Boolean getByUserId(@RequestBody UserDTO userDTO) {

        return iUserRpc.updateById(userDTO);
    }
}
