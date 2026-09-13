package com.bravetest.controller;

import com.bravetest.common.BusinessException;
import com.bravetest.common.R;
import com.bravetest.common.UserContext;
import com.bravetest.entity.PurificationBatch;
import com.bravetest.mapper.PurificationBatchMapper;
import com.bravetest.service.FinanceService;
import com.bravetest.service.SettlementService;
import com.bravetest.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Tag(name = "09-管理员后台")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TaskService taskService;
    private final SettlementService settlementService;
    private final FinanceService financeService;
    private final PurificationBatchMapper batchMapper;
    private final com.bravetest.service.ShopService shopService;
    private final com.bravetest.service.KnowledgeService knowledgeService;
    private final com.bravetest.mapper.KnowledgeContributionMapper contributionMapper;

    @Operation(summary = "总管理员发布行者善事任务")
    @PostMapping("/tasks/xingzhe")
    public R<Long> publishXingzhe(@RequestParam String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(defaultValue = "D") String level,
                                  @RequestParam(defaultValue = "100") Long rewardGold,
                                  @RequestParam(defaultValue = "10") Integer purifyValue) {
        requireRole("SUPER_ADMIN");
        return R.ok(taskService.publishXingzheTaskBySuper(
                UserContext.getUserId(), title, description, level, rewardGold, purifyValue));
    }

    @Operation(summary = "总管理员提供善事任务（批量示例入参见文档）")
    @PostMapping("/tasks")
    public R<Long> publishTask(@RequestParam String title,
                               @RequestParam(required = false) String description,
                               @RequestParam(defaultValue = "D") String level,
                               @RequestParam(defaultValue = "100") Long rewardGold,
                               @RequestParam(defaultValue = "10") Integer purifyValue) {
        requireRole("SUPER_ADMIN");
        return R.ok(taskService.publishXingzheTaskBySuper(
                UserContext.getUserId(), title, description, level, rewardGold, purifyValue));
    }

    @Operation(summary = "冒险者协会管理员：审核冒险者发布的任务")
    @PostMapping("/adventurer-tasks/{id}/audit")
    public R<Void> auditTask(@PathVariable Long id,
                             @RequestParam boolean pass,
                             @RequestParam(required = false) String remark) {
        requireRole("ADVENTURER_ADMIN", "SUPER_ADMIN");
        taskService.auditAdventurerTask(UserContext.getUserId(), id, pass, remark);
        return R.ok();
    }

    @Operation(summary = "行者协会管理员：审批任务帮助申请（通过即发布行者任务）")
    @PostMapping("/xingzhe-applies/{id}/audit")
    public R<Void> auditApply(@PathVariable Long id,
                              @RequestParam boolean pass,
                              @RequestParam(required = false) String remark) {
        requireRole("XINGZHE_ADMIN", "SUPER_ADMIN");
        taskService.auditXingzheApply(UserContext.getUserId(), id, pass, remark);
        return R.ok();
    }

    @Operation(summary = "总管理员：手动触发指定月份结算")
    @PostMapping("/settlement/monthly")
    public R<Void> settle(@RequestParam String month) {
        requireRole("SUPER_ADMIN");
        settlementService.settle(month);
        return R.ok();
    }

    @Operation(summary = "总管理员：生产净化剂（每月限100支，单支5万金币定价）")
    @PostMapping("/purifier/batch")
    public R<Long> producePurifier(@RequestParam Integer count) {
        requireRole("SUPER_ADMIN");
        if (count == null || count <= 0) {
            throw new BusinessException("生产数量必须大于0");
        }
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        // 校验当月累计产量不超过100支
        List<PurificationBatch> monthBatches = batchMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PurificationBatch>()
                        .eq(PurificationBatch::getProduceMonth, LocalDate.now()));
        int produced = monthBatches.stream().mapToInt(PurificationBatch::getRemaining).sum();
        // TODO(P3): 商城售卖后需引入 batch_total 字段或售卖流水，改为按产量而非剩余量校验月限额
        if (produced + count > 100) {
            throw new BusinessException("本月净化剂生产限额100支，已产 " + produced + " 支");
        }
        PurificationBatch batch = new PurificationBatch();
        batch.setBatchNo("PB-" + month.replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8));
        batch.setProduceMonth(LocalDate.now());
        batch.setUnitPrice(50000L);
        batch.setProducedBy(UserContext.getUserId());
        batch.setRemaining(count);
        batch.setCreatedAt(LocalDateTime.now());
        batchMapper.insert(batch);
        return R.ok(batch.getId());
    }

    @Operation(summary = "冒险者协会管理员：上架净化剂到商城")
    @PostMapping("/shop/purifier/list")
    public R<Long> listPurifier() {
        requireRole("ADVENTURER_ADMIN", "SUPER_ADMIN");
        return R.ok(shopService.listPurifier(UserContext.getUserId()));
    }

    @Operation(summary = "冒险者协会管理员：待审核任务列表")
    @GetMapping("/adventurer-tasks/pending")
    public R<java.util.List<com.bravetest.entity.Task>> pendingTasks() {
        requireRole("ADVENTURER_ADMIN", "SUPER_ADMIN");
        return R.ok(taskService.listPendingAudit());
    }

    @Operation(summary = "行者协会管理员：待审批申请列表")
    @GetMapping("/xingzhe-applies/pending")
    public R<java.util.List<com.bravetest.entity.XingzheApply>> pendingApplies() {
        requireRole("XINGZHE_ADMIN", "SUPER_ADMIN");
        return R.ok(taskService.listPendingApplies());
    }

    @Operation(summary = "知识宝库管理员：查看待审投稿")
    @GetMapping("/knowledge/contributions/pending")
    public R<java.util.List<com.bravetest.entity.KnowledgeContribution>> pendingContributions() {
        requireRole("KNOWLEDGE_ADMIN", "SUPER_ADMIN");
        return R.ok(knowledgeService.listPending());
    }

    @Operation(summary = "知识宝库管理员：审批投稿（pass=true 封装为信息集并定价上架）")
    @PostMapping("/knowledge/contributions/{id}/package")
    public R<Long> packageContribution(@PathVariable Long id,
                                       @RequestParam boolean pass,
                                       @RequestParam(required = false) String summary,
                                       @RequestParam(required = false) Long price,
                                       @RequestParam(required = false) String remark) {
        requireRole("KNOWLEDGE_ADMIN", "SUPER_ADMIN");
        return R.ok(knowledgeService.packageContribution(UserContext.getUserId(), id, summary, price, pass, remark));
    }

    @Operation(summary = "知识宝库管理员：处置版权违规（二次售卖→踢出知识宝库）")
    @PostMapping("/knowledge/violations")
    public R<Void> reportViolation(@RequestParam Long offenderUserId,
                                   @RequestParam(required = false) Long evidenceRef) {
        requireRole("KNOWLEDGE_ADMIN", "SUPER_ADMIN");
        knowledgeService.reportViolation(UserContext.getUserId(), offenderUserId, evidenceRef);
        return R.ok();
    }

    @Operation(summary = "知识宝库管理员：待处理举报列表")
    @GetMapping("/knowledge/reports/pending")
    public R<java.util.List<com.bravetest.entity.ViolationReport>> pendingReports() {
        requireRole("KNOWLEDGE_ADMIN", "SUPER_ADMIN");
        return R.ok(knowledgeService.listPendingReports());
    }

    @Operation(summary = "知识宝库管理员：处理举报（penalize=true 踢出被举报人，否则驳回）")
    @PostMapping("/knowledge/reports/{id}/handle")
    public R<Void> handleReport(@PathVariable Long id,
                                @RequestParam boolean penalize,
                                @RequestParam(required = false) String remark) {
        requireRole("KNOWLEDGE_ADMIN", "SUPER_ADMIN");
        knowledgeService.handleReport(UserContext.getUserId(), id, penalize, remark);
        return R.ok();
    }

    private void requireRole(String... allowed) {
        String role = UserContext.getRole();
        for (String a : allowed) {
            if (a.equals(role)) {
                return;
            }
        }
        throw new BusinessException(403, "无权限执行该操作");
    }
}
