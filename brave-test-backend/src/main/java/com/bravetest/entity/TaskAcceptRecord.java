package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * task_accept_record
 */
@Data
@TableName("task_accept_record")
public class TaskAcceptRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long taskId;

    /** 冒险者档案id */
    private Long adventurerId;

    private Long adventurerUserId;

    /** 是否越级接单 */
    private Integer isCrossLevel;

    /** 0进行中 1成功 2失败 */
    private Integer status;

    /** 结算金币快照 */
    private Long goldPaid;

    /** 净化值快照 */
    private Integer purifyGained;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
