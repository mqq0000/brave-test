package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * borrow_record
 */
@Data
@TableName("borrow_record")
public class BorrowRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long infoSetId;

    private Long userId;

    /** 1日卡 2周卡 3月卡 */
    private Integer cardType;

    private Long price;

    private LocalDateTime startAt;

    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
}
