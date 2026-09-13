package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 自我商城兑换记录：兑换扣币，10% 会费入冒险者协会账本
 */
@Data
@TableName("self_redemption")
public class SelfRedemption {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long itemId;
    private String itemName;
    private Integer cost;
    private Integer assocFee;
    private LocalDateTime redeemedAt;
}
