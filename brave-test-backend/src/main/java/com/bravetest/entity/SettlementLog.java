package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * settlement_log
 */
@Data
@TableName("settlement_log")
public class SettlementLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 结算月份yyyy-MM */
    private String month;

    /** 1佣金上缴60% 2知识宝库拨款40% 3净化剂生产 */
    private Integer type;

    private Long amount;

    private Integer fromAccount;

    private Integer toAccount;

    private LocalDateTime createdAt;
}
