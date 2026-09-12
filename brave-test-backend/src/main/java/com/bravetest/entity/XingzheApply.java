package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * xingzhe_apply
 */
@Data
@TableName("xingzhe_apply")
public class XingzheApply {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 申请人userId */
    private Long applicantId;

    private String title;

    private String content;

    /** 0待审 1通过 2驳回 */
    private Integer auditStatus;

    private Long auditorId;

    private String auditRemark;

    /** 通过后生成的行者任务id */
    private Long publishedTaskId;

    private LocalDateTime createdAt;
}
