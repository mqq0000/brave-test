package com.bravetest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bravetest.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** user Mapper */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
