package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * test_result
 */
@Data
@TableName("test_result")
public class TestResult {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;

    private Long adventurerId;

    /** 作答JSON */
    private String answers;

    /** 勇气分 */
    private Integer traitCourage;

    /** 理性分 */
    private Integer traitRationality;

    /** 仁善分 */
    private Integer traitKindness;

    /** 负面选择累计的初始污染 */
    private Integer pollutionDelta;

    /** 觉醒称号 */
    private String title;

    private LocalDateTime createdAt;
}
