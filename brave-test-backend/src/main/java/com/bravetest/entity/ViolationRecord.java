package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * violation_record
 */
@Data
@TableName("violation_record")
public class ViolationRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;

    /** 1二次售卖/传播牟利 */
    private Integer type;

    private String evidenceUrl;

    /** 1踢出知识宝库 */
    private Integer penalty;

    private Long handlerId;

    private LocalDateTime createdAt;
}
