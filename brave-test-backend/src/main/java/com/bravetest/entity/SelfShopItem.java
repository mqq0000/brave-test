package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 自我奖励商城商品：冒险者自己添加的现实奖励，用任务赚的虚拟币兑换
 */
@Data
@TableName("self_shop_item")
public class SelfShopItem {

    public static final int ST_ON = 1;
    public static final int ST_OFF = 2;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Integer cost;
    private Integer status;
    private LocalDateTime createdAt;
}
