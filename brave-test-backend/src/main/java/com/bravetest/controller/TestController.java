package com.bravetest.controller;

import com.bravetest.common.R;
import com.bravetest.common.UserContext;
import com.bravetest.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "07-觉醒测试")
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @Operation(summary = "觉醒测试题目列表")
    @GetMapping("/questions")
    public R<List<Map<String, Object>>> questions() {
        return R.ok(testService.listQuestions());
    }

    @Operation(summary = "提交作答（返回觉醒称号与特质分，负面选项将染污萌芽）")
    @PostMapping("/submit")
    public R<Map<String, Object>> submit(@RequestBody List<Map<String, Object>> answers) {
        return R.ok(testService.submit(UserContext.getUserId(), answers));
    }

    @Operation(summary = "我的最新觉醒结果")
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(testService.latestResult(UserContext.getUserId()));
    }
}
