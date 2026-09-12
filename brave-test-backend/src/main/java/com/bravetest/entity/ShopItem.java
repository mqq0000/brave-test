package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * shop_item
 */
@Data
@TableName("shop_item")
public class ShopItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;

    /** 1普通 2净化剂 */
    private Integer itemType;

    private Long price;

    private Integer stock;

    /** 上架管理员userId */
    private Long sellerId;

    /** 0下架 1在售 */
    private Integer status;

    private String imageUrl;

    private LocalDateTime createdAt;
}
