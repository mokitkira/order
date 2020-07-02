package com.kira.order.mapper;

import com.kira.order.entity.SubOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface SubOrderMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SubOrder record);

    int insertSelective(SubOrder record);

    SubOrder selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(SubOrder record);

    int updateByPrimaryKey(SubOrder record);

    Boolean existLatestSubCallOrder(@Param("orderNum") String orderNum,@Param("userName") String userName);
}