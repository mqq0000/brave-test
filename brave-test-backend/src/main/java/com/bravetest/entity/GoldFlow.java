package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * gold_flow
 */
@Data
@TableName("gold_flow")
public class GoldFlow {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;

    /** TASK_REWARD/COMMISSION/ESCROW/LEVEL_UP_REWARD/SHOP_BUY/PURIFIER/REFUND/TRANSFER_UP/GRANT */
    private String bizType;

    /** 正收入 负支出 */
    private Long amount;

    private Long balanceAfter;

    private String refType;

    private Long refId;

    private LocalDateTime createdAt;
}
