package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * association_ledger
 */
@Data
@TableName("association_ledger")
public class AssociationLedger {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 1行者 2冒险者 3知识宝库 4总管理员 */
    private Integer association;

    /** yyyy-MM */
    private String month;

    private Long income;

    private Long expense;

    private Long balance;

    private LocalDateTime updatedAt;
}
