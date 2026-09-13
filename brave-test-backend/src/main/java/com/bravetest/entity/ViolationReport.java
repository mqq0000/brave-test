package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * violation_report 知识宝库举报表
 */
@Data
@TableName("violation_report")
public class ViolationReport {

    /** 0待处理 1已处罚 2已驳回 */
    public static final int ST_PENDING = 0;
    public static final int ST_PENALIZED = 1;
    public static final int ST_DISMISSED = 2;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 举报人 userId */
    private Long reporterId;

    /** 被举报人 userId */
    private Long offenderId;

    /** 关联信息集（可空） */
    private Long infoSetId;

    /** 举报说明 */
    private String description;

    /** 0待处理 1已处罚 2已驳回 */
    private Integer status;

    /** 处理管理员 userId */
    private Long handlerId;

    private String handleRemark;

    private LocalDateTime createdAt;
}
