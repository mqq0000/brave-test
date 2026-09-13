package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * knowledge_favorite 知识宝库收藏表
 */
@Data
@TableName("knowledge_favorite")
public class KnowledgeFavorite {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long infoSetId;

    private LocalDateTime createdAt;
}
