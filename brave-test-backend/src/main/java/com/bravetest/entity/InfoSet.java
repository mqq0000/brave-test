package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * info_set
 */
@Data
@TableName("info_set")
public class InfoSet {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long contributionId;

    private Long contributorId;

    private String title;

    private String summary;

    /** MinIO对象key */
    private String contentUrl;

    /** 估价定价 */
    private Long price;

    /** 0下架 1在售 */
    private Integer status;

    private Integer soldCount;

    private LocalDateTime createdAt;
}
