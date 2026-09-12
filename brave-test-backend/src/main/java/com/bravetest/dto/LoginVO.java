package com.bravetest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "JWT令牌")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "角色")
    private String role;
}
