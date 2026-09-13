package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 技能树节点：完成任务涨经验，每 100 经验升 1 级
 */
@Data
@TableName("skill")
public class Skill {

    /** 每级所需经验 */
    public static final int EXP_PER_LEVEL = 100;
    /** 等级上限 */
    public static final int MAX_LEVEL = 9;
    /** 在节点下开枝（创建子技能）费用 */
    public static final long BRANCH_COST = 50;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    /** 父技能，NULL为根 */
    private Long parentId;
    private String name;
    private Integer exp;
    private LocalDateTime createdAt;

    /** 由经验推导等级（不落库，避免漂移） */
    public int level() {
        int e = exp == null ? 0 : exp;
        return Math.min(MAX_LEVEL, e / EXP_PER_LEVEL + 1);
    }
}
