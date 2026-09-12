package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * task
 */
@Data
@TableName("task")
public class Task {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 1行者协会 2冒险者协会 */
    private Integer sourceType;

    /** 发布者userId */
    private Long publisherId;

    private String title;

    private String description;

    /** D/C/B/A/S */
    private String taskLevel;

    /** 报酬(发布者托管金额) */
    private Long rewardGold;

    /** 接取者所得(=90%) */
    private Long acceptorGold;

    /** 净化值 */
    private Integer purifyValue;

    /** 0待审核 1发布中 2进行中 3待验收 4完成 5失败 6驳回 7下架 */
    private Integer status;

    /** 接取者userId */
    private Long acceptorId;

    private LocalDateTime acceptDeadline;

    private LocalDateTime finishDeadline;

    /** 审核人userId */
    private Long auditId;

    private String auditRemark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
