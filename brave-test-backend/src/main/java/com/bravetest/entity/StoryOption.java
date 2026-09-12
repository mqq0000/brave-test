package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * story_option
 */
@Data
@TableName("story_option")
public class StoryOption {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 所属节点 */
    private Long nodeId;

    /** 选项文本 */
    private String text;

    /** 污染变化 正=染污 负=净化 */
    private Integer pollutionDelta;

    /** 金币变化 */
    private Long goldDelta;

    /** 下一节点 NULL=本章完结 */
    private Long nextNodeId;

    /** 展示顺序 */
    private Integer seq;
}
