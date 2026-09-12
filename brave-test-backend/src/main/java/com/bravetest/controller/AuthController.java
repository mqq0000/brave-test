package com.bravetest.controller;

import com.bravetest.common.R;
import com.bravetest.dto.LoginDTO;
import com.bravetest.dto.LoginVO;
import com.bravetest.dto.RegisterDTO;
import com.bravetest.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01-认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "注册（自动创建D级冒险者档案）")
    @PostMapping("/register")
    public R<Long> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(authService.register(dto));
    }

    @Operation(summary = "登录，返回JWT")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "登出（JWT加入Redis黑名单）")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader.replaceFirst("Bearer ", ""));
        return R.ok();
    }
}
