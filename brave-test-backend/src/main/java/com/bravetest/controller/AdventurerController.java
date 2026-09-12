package com.bravetest.controller;

import com.bravetest.common.R;
import com.bravetest.common.UserContext;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.GoldFlow;
import com.bravetest.mapper.GoldFlowMapper;
import com.bravetest.service.AdventurerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "02-冒险者档案")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdventurerController {

    private final AdventurerService adventurerService;
    private final GoldFlowMapper goldFlowMapper;

    @Operation(summary = "我的档案：等级/污染/萌芽状态/金币/封禁")
    @GetMapping("/adventurer/me")
    public R<Adventurer> me() {
        return R.ok(adventurerService.getByUserId(UserContext.getUserId()));
    }

    @Operation(summary = "完成任务数排行榜 Top20（Redis ZSet）")
    @GetMapping("/adventurer/rank")
    public R<java.util.List<java.util.Map<String, Object>>> rank() {
        return R.ok(adventurerService.topRank(20));
    }

    @Operation(summary = "我的金币流水")
    @GetMapping("/finance/flows")
    public R<List<GoldFlow>> flows() {
        return R.ok(goldFlowMapper.selectList(new LambdaQueryWrapper<GoldFlow>()
                .eq(GoldFlow::getUserId, UserContext.getUserId())
                .orderByDesc(GoldFlow::getCreatedAt)
                .last("LIMIT 50")));
    }
}
