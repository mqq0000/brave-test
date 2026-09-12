package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * player_story_progress
 */
@Data
@TableName("player_story_progress")
public class PlayerStoryProgress {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long adventurerId;

    private Long userId;

    private Long chapterId;

    private Long currentNodeId;

    /** 0进行中 1已完成 */
    private Integer status;

    /** 选择记录JSON */
    private String choicesLog;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
