package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * purification_batch
 */
@Data
@TableName("purification_batch")
public class PurificationBatch {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 批次号 */
    private String batchNo;

    /** 生产月份(每月限100支) */
    private LocalDate produceMonth;

    /** 定价50000 */
    private Long unitPrice;

    /** 总管理员userId */
    private Long producedBy;

    /** 本批剩余 */
    private Integer remaining;

    private LocalDateTime createdAt;
}
