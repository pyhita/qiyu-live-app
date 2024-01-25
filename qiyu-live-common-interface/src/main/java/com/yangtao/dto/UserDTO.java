package com.yangtao.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDTO implements Serializable {



    private Long userId;
    private String nickName;
    private String trueName;
    private String avatar;
    private Integer sex;
    private Integer workCity;
    private Integer bornCity;
}
