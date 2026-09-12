package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * trade_order
 */
@Data
@TableName("trade_order")
public class TradeOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long itemId;

    private Long buyerId;

    private Long sellerId;

    private Long price;

    /** 1普通购买 2净化剂 */
    private Integer type;

    /** 0待支付 1已完成 2已取消 */
    private Integer status;

    private LocalDateTime createdAt;
}
