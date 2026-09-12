package com.bravetest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度3~32")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度6~32")
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称最长32")
    @Schema(description = "昵称（即冒险者名号）")
    private String nickname;
}
