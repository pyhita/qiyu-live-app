package com.yangtao.dao.po;

import lombok.Data;

@Data
public class UserPO {

    private Long userId;
    private String nickName;
    private String trueName;
    private String avatar;
    private Integer sex;
    private Integer workCity;
    private Integer bornCity;
}
