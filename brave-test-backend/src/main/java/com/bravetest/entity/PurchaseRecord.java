package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * purchase_record
 */
@Data
@TableName("purchase_record")
public class PurchaseRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long infoSetId;

    private Long buyerId;

    private Long price;

    private LocalDateTime purchasedAt;
}
