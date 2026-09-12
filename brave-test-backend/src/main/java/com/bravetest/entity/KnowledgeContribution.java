package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * knowledge_contribution
 */
@Data
@TableName("knowledge_contribution")
public class KnowledgeContribution {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long contributorId;

    private String title;

    private String content;

    /** 隐私声明 0无 1已声明不侵犯隐私 */
    private Integer privacyDeclared;

    /** 0待审 1通过 2驳回 */
    private Integer auditStatus;

    private Long auditorId;

    private String auditRemark;

    /** 封装后回填 */
    private Long infoSetId;

    private LocalDateTime createdAt;
}
