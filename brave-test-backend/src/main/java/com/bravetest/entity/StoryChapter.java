package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * story_chapter
 */
@Data
@TableName("story_chapter")
public class StoryChapter {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 章节序号(解锁链) */
    private Integer seq;

    /** 章节标题 */
    private String title;

    private LocalDateTime createdAt;
}
