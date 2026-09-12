package com.bravetest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bravetest.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/** task Mapper */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
