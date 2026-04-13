package com.cinema.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cinema.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
