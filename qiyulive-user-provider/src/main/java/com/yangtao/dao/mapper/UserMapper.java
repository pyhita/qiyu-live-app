package com.yangtao.dao.mapper;

import com.yangtao.dao.po.UserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM t_user WHERE user_id = #{userId}")
    UserPO selectById(Long userId);

    List<UserPO> selectByIdList(List<Long> userIdList);

    int updateById(UserPO userPO);
}
