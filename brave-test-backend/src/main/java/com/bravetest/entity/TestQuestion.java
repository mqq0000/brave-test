package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * test_question
 */
@Data
@TableName("test_question")
public class TestQuestion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 题号 */
    private Integer seq;

    /** 题干 */
    private String content;

    /** 选项JSON [{text,courage,rationality,kindness,pollution}] */
    private String options;

    /** 0下线 1启用 */
    private Integer status;
}
