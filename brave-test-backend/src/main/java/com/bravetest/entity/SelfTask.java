package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 自我任务（修身·自我激励）：自己给自己发任务，完成后获得虚拟币与技能经验
 */
@Data
@TableName("self_task")
public class SelfTask {

    public static final int ST_ACTIVE = 1;
    public static final int ST_DONE = 2;
    public static final int ST_ABANDONED = 3;
    public static final int REPEAT_ONCE = 0;
    public static final int REPEAT_DAILY = 1;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String title;
    private String description;

    /** 完成奖励虚拟币 */
    private Integer coinReward;
    /** 关联技能，完成涨经验 */
    private Long skillId;
    /** 0一次性 1每日 */
    private Integer repeatType;
    private Integer status;
    private LocalDateTime lastCompletedAt;
    /** 连续打卡天数（每日任务） */
    private Integer streakDays;
    private LocalDateTime createdAt;
}
