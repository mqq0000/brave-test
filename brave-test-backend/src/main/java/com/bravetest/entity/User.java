package com.bravetest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * user
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 登录名 */
    private String username;

    /** BCrypt密文 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像(MinIO) */
    private String avatarUrl;

    /** ADVENTURER/XINGZHE_ADMIN/ADVENTURER_ADMIN/KNOWLEDGE_ADMIN/SUPER_ADMIN */
    private String role;

    /** 0正常 1禁用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
