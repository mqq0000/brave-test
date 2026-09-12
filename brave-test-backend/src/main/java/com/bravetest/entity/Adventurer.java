package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * adventurer
 */
@Data
@TableName("adventurer")
public class Adventurer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 1:1关联user */
    private Long userId;

    /** D/C/B/A/S */
    private String rankLevel;

    /** 累计完成任务数 */
    private Integer completedCount;

    /** 污染值0~100 */
    private Integer pollutionValue;

    /** NORMAL/GRAY/RED */
    private String sproutStatus;

    /** 金币余额 */
    private Long goldBalance;

    /** 协会封禁截止，NULL=正常 */
    private LocalDateTime bannedUntil;

    /** 知识宝库封禁 0否 1是 */
    private Integer knowledgeBanned;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
