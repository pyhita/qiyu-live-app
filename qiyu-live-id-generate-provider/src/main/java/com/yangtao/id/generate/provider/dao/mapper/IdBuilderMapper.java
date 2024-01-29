package com.yangtao.id.generate.provider.dao.mapper;

import com.yangtao.id.generate.provider.dao.po.IdBuilderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface IdBuilderMapper {

    @Update("update t_id_generate_config set next_threshold=next_threshold+step," +
            "current_start=current_start+step,version=version+1 where id =#{id} and version=#{version}")
    int updateNewIdCountAndVersion(@Param("id")int id, @Param("version")int version);

    @Select("select * from t_id_generate_config")
    List<IdBuilderPO> selectAll();

    @Select("SELECT * FROM t_id_generate_config WHERE id = #{id}")
    IdBuilderPO selectById(Integer id);
}
