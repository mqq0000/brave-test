package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * purification_record
 */
@Data
@TableName("purification_record")
public class PurificationRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long adventurerId;

    /** XINGZHE_TASK/PURIFIER/KNOWLEDGE */
    private String source;

    private Integer valueBefore;

    private Integer valueAfter;

    /** 关联来源id */
    private Long refId;

    private LocalDateTime createdAt;
}
