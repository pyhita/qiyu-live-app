package com.yangtao.request;

import lombok.Data;

import java.util.List;

@Data
public class UserListRequest {

    private List<Long> userIdList;
}
