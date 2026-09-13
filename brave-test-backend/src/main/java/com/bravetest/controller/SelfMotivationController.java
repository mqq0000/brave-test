package com.bravetest.controller;

import com.bravetest.common.R;
import com.bravetest.common.UserContext;
import com.bravetest.entity.SelfRedemption;
import com.bravetest.entity.SelfShopItem;
import com.bravetest.entity.SelfTask;
import com.bravetest.service.SelfMotivationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 修身·自我激励系统：自我任务 / 技能树 / 自我奖励商城
 */
@Tag(name = "修身养成（自我激励）")
@RestController
@RequestMapping("/api/v1/self")
@RequiredArgsConstructor
public class SelfMotivationController {

    private final SelfMotivationService service;

    // ==================== 自我任务 ====================

    @Operation(summary = "我的自我任务列表")
    @GetMapping("/tasks")
    public R<List<SelfTask>> tasks() {
        return R.ok(service.listTasks(UserContext.getUserId()));
    }

    @Operation(summary = "创建自我任务（coinReward 1~100，repeatType: 0一次性 1每日）")
    @PostMapping("/tasks")
    public R<Long> createTask(@RequestBody Map<String, Object> body) {
        Long skillId = body.get("skillId") == null || String.valueOf(body.get("skillId")).isBlank()
                ? null : Long.valueOf(String.valueOf(body.get("skillId")));
        return R.ok(service.createTask(UserContext.getUserId(),
                (String) body.get("title"),
                (String) body.get("description"),
                intVal(body.get("coinReward"), 10),
                skillId,
                intVal(body.get("repeatType"), 0)));
    }

    @Operation(summary = "完成任务：发币+技能经验+净化萌芽")
    @PostMapping("/tasks/{id}/complete")
    public R<Map<String, Object>> completeTask(@PathVariable Long id) {
        return R.ok(service.completeTask(UserContext.getUserId(), id));
    }

    @Operation(summary = "放弃任务")
    @PostMapping("/tasks/{id}/abandon")
    public R<Void> abandonTask(@PathVariable Long id) {
        service.abandonTask(UserContext.getUserId(), id);
        return R.ok();
    }

    // ==================== 技能树 ====================

    @Operation(summary = "我的技能树（level 由经验推导，每100经验升1级）")
    @GetMapping("/skills")
    public R<List<Map<String, Object>>> skills() {
        return R.ok(service.skillTree(UserContext.getUserId()));
    }

    @Operation(summary = "初始化默认技能树（专注/健体/心性 三系）")
    @PostMapping("/skills/init")
    public R<Integer> initSkills() {
        return R.ok(service.initSkills(UserContext.getUserId()));
    }

    @Operation(summary = "开枝：在节点下创建子技能（花费50金币，全额入冒险者协会）")
    @PostMapping("/skills")
    public R<Long> branchSkill(@RequestBody Map<String, Object> body) {
        return R.ok(service.branchSkill(UserContext.getUserId(),
                Long.valueOf(String.valueOf(body.get("parentId"))),
                (String) body.get("name")));
    }

    // ==================== 自我奖励商城 ====================

    @Operation(summary = "我的商城商品")
    @GetMapping("/shop/items")
    public R<List<SelfShopItem>> items() {
        return R.ok(service.listItems(UserContext.getUserId()));
    }

    @Operation(summary = "添加自我奖励商品（现实奖励，cost 1~100000）")
    @PostMapping("/shop/items")
    public R<Long> addItem(@RequestBody Map<String, Object> body) {
        return R.ok(service.addItem(UserContext.getUserId(),
                (String) body.get("name"),
                (String) body.get("description"),
                intVal(body.get("cost"), 1)));
    }

    @Operation(summary = "商品下架")
    @PostMapping("/shop/items/{id}/offshelf")
    public R<Void> offShelf(@PathVariable Long id) {
        service.offShelfItem(UserContext.getUserId(), id);
        return R.ok();
    }

    @Operation(summary = "兑换奖励：扣币，10% 会费入冒险者协会")
    @PostMapping("/shop/items/{id}/redeem")
    public R<SelfRedemption> redeem(@PathVariable Long id) {
        return R.ok(service.redeem(UserContext.getUserId(), id));
    }

    @Operation(summary = "我的兑换记录")
    @GetMapping("/redemptions")
    public R<List<SelfRedemption>> redemptions() {
        return R.ok(service.listRedemptions(UserContext.getUserId()));
    }

    private static int intVal(Object o, int def) {
        if (o == null) {
            return def;
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
