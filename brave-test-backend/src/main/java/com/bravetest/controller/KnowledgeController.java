package com.bravetest.controller;

import com.bravetest.common.R;
import com.bravetest.common.UserContext;
import com.bravetest.entity.BorrowRecord;
import com.bravetest.entity.InfoSet;
import com.bravetest.entity.PurchaseRecord;
import com.bravetest.mapper.BorrowRecordMapper;
import com.bravetest.mapper.PurchaseRecordMapper;
import com.bravetest.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "05-知识宝库")
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final PurchaseRecordMapper purchaseRecordMapper;
    private final BorrowRecordMapper borrowRecordMapper;

    @Operation(summary = "提交独有经验（不得侵犯他人隐私）")
    @PostMapping("/contributions")
    public R<Long> contribute(@RequestParam String title, @RequestParam String content) {
        return R.ok(knowledgeService.contribute(UserContext.getUserId(), title, content));
    }

    @Operation(summary = "我的投稿记录")
    @GetMapping("/contributions/mine")
    public R<List<com.bravetest.entity.KnowledgeContribution>> myContributions() {
        return R.ok(knowledgeService.myContributions(UserContext.getUserId()));
    }

    @Operation(summary = "信息集列表")
    @GetMapping("/info-sets")
    public R<List<InfoSet>> infoSets(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size) {
        return R.ok(knowledgeService.listInfoSets(page, size));
    }

    @Operation(summary = "购买信息集（永久阅读权，一经售卖不可二次售卖）")
    @PostMapping("/info-sets/{id}/purchase")
    public R<Long> purchase(@PathVariable Long id) {
        return R.ok(knowledgeService.purchase(UserContext.getUserId(), id));
    }

    @Operation(summary = "借阅信息集（cardType: 1日卡10% 2周卡25% 3月卡50%）")
    @PostMapping("/info-sets/{id}/borrow")
    public R<Long> borrow(@PathVariable Long id, @RequestParam int cardType) {
        return R.ok(knowledgeService.borrow(UserContext.getUserId(), id, cardType));
    }

    @Operation(summary = "阅读信息集内容（返回临时URL或文本，校验购买/借阅有效期）")
    @GetMapping("/info-sets/{id}/content")
    public R<com.bravetest.service.KnowledgeService.ReadVO> read(@PathVariable Long id) {
        return R.ok(knowledgeService.read(UserContext.getUserId(), id));
    }

    @Operation(summary = "我的购买记录")
    @GetMapping("/purchases/mine")
    public R<List<PurchaseRecord>> myPurchases() {
        return R.ok(purchaseRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PurchaseRecord>()
                        .eq(PurchaseRecord::getBuyerId, UserContext.getUserId())
                        .orderByDesc(PurchaseRecord::getPurchasedAt)));
    }

    @Operation(summary = "我的借阅记录")
    @GetMapping("/borrows/mine")
    public R<List<BorrowRecord>> myBorrows() {
        return R.ok(borrowRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, UserContext.getUserId())
                        .orderByDesc(BorrowRecord::getCreatedAt)));
    }
}
