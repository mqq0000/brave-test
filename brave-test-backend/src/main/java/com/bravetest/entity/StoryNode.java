package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * story_node
 */
@Data
@TableName("story_node")
public class StoryNode {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long chapterId;

    /** NARRATE叙述/CHOICE抉择/END终章 */
    private String nodeType;

    /** 剧情文本 */
    private String content;

    /** 章内顺序 */
    private Integer seq;
}
