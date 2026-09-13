package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.BorrowRecord;
import com.bravetest.entity.InfoSet;
import com.bravetest.entity.KnowledgeContribution;
import com.bravetest.entity.PurchaseRecord;
import com.bravetest.entity.ViolationRecord;
import com.bravetest.mapper.AdventurerMapper;
import com.bravetest.mapper.BorrowRecordMapper;
import com.bravetest.mapper.InfoSetMapper;
import com.bravetest.mapper.KnowledgeContributionMapper;
import com.bravetest.mapper.PurchaseRecordMapper;
import com.bravetest.mapper.ViolationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 知识宝库服务：投稿 / 封装定价 / 购买 / 借阅（日/周/月卡）/ 版权违规处置
 * <p>经济规则：购买/借阅收入 70% 分给贡献者，30% 入知识宝库账本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    /** 借阅卡类型 */
    public static final int CARD_DAY = 1;
    public static final int CARD_WEEK = 2;
    public static final int CARD_MONTH = 3;
    /** 借阅价相对购买价比例：1天10% / 周25% / 月50% */
    private static final double[] CARD_RATE = {0.0, 0.10, 0.25, 0.50};
    private static final long[] CARD_DAYS = {0, 1, 7, 30};

    /** 贡献者分成比例 */
    private static final double CONTRIBUTOR_SHARE = 0.70;

    private final KnowledgeContributionMapper contributionMapper;
    private final InfoSetMapper infoSetMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final PurchaseRecordMapper purchaseRecordMapper;
    private final ViolationRecordMapper violationRecordMapper;
    private final com.bravetest.mapper.ViolationReportMapper violationReportMapper;
    private final com.bravetest.mapper.KnowledgeFavoriteMapper favoriteMapper;
    private final AdventurerMapper adventurerMapper;
    private final com.bravetest.mapper.PurificationRecordMapper purificationRecordMapper;
    private final FinanceService financeService;
    private final MinioService minioService;

    /** 阅读结果：type=text(直接文本) / url(MinIO临时签名URL) */
    public record ReadVO(String type, String value) {
    }

    // ==================== 投稿 ====================

    public Long contribute(Long userId, String title, String content) {
        Adventurer adv = adventurerMapper.selectOne(
                new LambdaQueryWrapper<Adventurer>().eq(Adventurer::getUserId, userId));
        if (adv != null && adv.getKnowledgeBanned() != null && adv.getKnowledgeBanned() == 1) {
            throw new BusinessException(403, "已被知识宝库处置，永久失去进入知识宝库的权利");
        }
        KnowledgeContribution c = new KnowledgeContribution();
        c.setContributorId(userId);
        c.setTitle(title);
        c.setContent(content);
        c.setPrivacyDeclared(1);
        c.setAuditStatus(0);
        c.setCreatedAt(LocalDateTime.now());
        contributionMapper.insert(c);
        return c.getId();
    }

    public List<KnowledgeContribution> myContributions(Long userId) {
        return contributionMapper.selectList(new LambdaQueryWrapper<KnowledgeContribution>()
                .eq(KnowledgeContribution::getContributorId, userId)
                .orderByDesc(KnowledgeContribution::getCreatedAt));
    }

    public List<KnowledgeContribution> listPending() {
        return contributionMapper.selectList(new LambdaQueryWrapper<KnowledgeContribution>()
                .eq(KnowledgeContribution::getAuditStatus, 0)
                .orderByAsc(KnowledgeContribution::getCreatedAt));
    }

    /**
     * 管理员筛查：驳回 或 通过并封装为信息集定价上架
     */
    @Transactional
    public Long packageContribution(Long adminUserId, Long contributionId,
                                    String summary, Long price, boolean pass, String remark) {
        KnowledgeContribution c = contributionMapper.selectById(contributionId);
        if (c == null) {
            throw new BusinessException("投稿不存在");
        }
        if (c.getAuditStatus() != 0) {
            throw new BusinessException("该投稿已处理");
        }
        c.setAuditorId(adminUserId);
        c.setAuditRemark(remark);
        if (!pass) {
            c.setAuditStatus(2);
            contributionMapper.updateById(c);
            return null;
        }
        c.setAuditStatus(1);
        contributionMapper.updateById(c);

        InfoSet infoSet = new InfoSet();
        infoSet.setContributionId(contributionId);
        infoSet.setContributorId(c.getContributorId());
        infoSet.setTitle(c.getTitle());
        infoSet.setSummary(summary);
        infoSet.setPrice(price);
        infoSet.setStatus(1);
        infoSet.setSoldCount(0);
        infoSet.setCreatedAt(LocalDateTime.now());
        infoSetMapper.insert(infoSet);

        c.setInfoSetId(infoSet.getId());
        contributionMapper.updateById(c);

        // 内容上传 MinIO（失败则回退数据库文本读取）
        try {
            String objectKey = "info-set/" + infoSet.getId() + ".txt";
            if (minioService.putText(objectKey, c.getContent())) {
                infoSetMapper.update(null, new LambdaUpdateWrapper<InfoSet>()
                        .eq(InfoSet::getId, infoSet.getId())
                        .set(InfoSet::getContentUrl, objectKey));
                infoSet.setContentUrl(objectKey);
            }
        } catch (Exception e) {
            log.warn("信息集内容上传 MinIO 失败，回退数据库: {}", e.getMessage());
        }

        // 知识收录也净化萌芽
        Adventurer contributor = adventurerMapper.selectOne(
                new LambdaQueryWrapper<Adventurer>().eq(Adventurer::getUserId, c.getContributorId()));
        if (contributor != null) {
            try {
                // 未使用注入 mapper 以避免循环依赖，直接调用净化
                adventurerPurify(contributor, 5, "KNOWLEDGE", infoSet.getId());
            } catch (Exception e) {
                log.warn("知识收录净化萌芽失败: {}", e.getMessage());
            }
        }
        return infoSet.getId();
    }

    private void adventurerPurify(Adventurer adv, int value, String source, Long refId) {
        int before = adv.getPollutionValue() == null ? 0 : adv.getPollutionValue();
        int after = Math.max(0, before - value);
        adventurerMapper.update(null, new LambdaUpdateWrapper<Adventurer>()
                .eq(Adventurer::getId, adv.getId())
                .set(Adventurer::getPollutionValue, after)
                .set(Adventurer::getSproutStatus, AdventurerService.deriveSproutStatus(after)));
        com.bravetest.entity.PurificationRecord record = new com.bravetest.entity.PurificationRecord();
        record.setAdventurerId(adv.getId());
        record.setSource(source);
        record.setValueBefore(before);
        record.setValueAfter(after);
        record.setRefId(refId);
        record.setCreatedAt(LocalDateTime.now());
        purificationRecordMapper.insert(record);
    }

    // ==================== 信息集 ====================

    public List<InfoSet> listInfoSets(long page, long size) {
        return listInfoSets(page, size, null);
    }

    /** 信息集列表（支持标题关键词搜索） */
    public List<InfoSet> listInfoSets(long page, long size, String keyword) {
        return infoSetMapper.selectList(new LambdaQueryWrapper<InfoSet>()
                .eq(InfoSet::getStatus, 1)
                .like(keyword != null && !keyword.isBlank(), InfoSet::getTitle, keyword)
                .orderByDesc(InfoSet::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size));
    }

    /**
     * 我的书架：已购买（永久）+ 有效借阅（含剩余天数）
     */
    public java.util.Map<String, Object> myShelf(Long userId) {
        java.util.Map<String, Object> shelf = new java.util.HashMap<>();

        List<Map<String, Object>> purchased = new java.util.ArrayList<>();
        for (PurchaseRecord p : purchaseRecordMapper.selectList(new LambdaQueryWrapper<PurchaseRecord>()
                .eq(PurchaseRecord::getBuyerId, userId)
                .orderByDesc(PurchaseRecord::getPurchasedAt))) {
            InfoSet is = infoSetMapper.selectById(p.getInfoSetId());
            if (is == null) continue;
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("infoSetId", is.getId());
            row.put("title", is.getTitle());
            row.put("summary", is.getSummary());
            row.put("price", is.getPrice());
            row.put("accessType", "PURCHASED");
            row.put("purchasedAt", p.getPurchasedAt());
            purchased.add(row);
        }

        List<Map<String, Object>> borrowing = new java.util.ArrayList<>();
        for (BorrowRecord b : borrowRecordMapper.selectList(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .gt(BorrowRecord::getExpireAt, LocalDateTime.now())
                .orderByDesc(BorrowRecord::getExpireAt))) {
            InfoSet is = infoSetMapper.selectById(b.getInfoSetId());
            if (is == null) continue;
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("infoSetId", is.getId());
            row.put("title", is.getTitle());
            row.put("summary", is.getSummary());
            row.put("price", is.getPrice());
            row.put("accessType", "BORROWING");
            row.put("cardType", b.getCardType());
            row.put("borrowId", b.getId());
            row.put("expireAt", b.getExpireAt());
            row.put("remainDays", java.time.Duration.between(LocalDateTime.now(), b.getExpireAt()).toDays());
            borrowing.add(row);
        }

        shelf.put("purchased", purchased);
        shelf.put("borrowing", borrowing);
        return shelf;
    }

    /**
     * 借阅续费：对未过期借阅按卡类型顺延，从当前到期时间起加天数
     */
    @Transactional
    public Long renewBorrow(Long userId, Long borrowId, int cardType) {
        if (cardType < CARD_DAY || cardType > CARD_MONTH) {
            throw new BusinessException("借阅卡类型非法");
        }
        BorrowRecord borrow = borrowRecordMapper.selectById(borrowId);
        if (borrow == null || !userId.equals(borrow.getUserId())) {
            throw new BusinessException(403, "无权操作该借阅记录");
        }
        if (borrow.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("借阅已过期，请重新借阅");
        }
        InfoSet infoSet = infoSetMapper.selectById(borrow.getInfoSetId());
        if (infoSet == null || infoSet.getStatus() != 1) {
            throw new BusinessException("信息集不存在或已下架");
        }
        long price = Math.max(1, Math.round(infoSet.getPrice() * CARD_RATE[cardType]));
        financeService.deductGold(requireNotKnowledgeBanned(userId).getId(), price,
                "KNOWLEDGE_RENEW", "INFO_SET", infoSet.getId());
        borrow.setExpireAt(borrow.getExpireAt().plusDays(CARD_DAYS[cardType]));
        borrowRecordMapper.updateById(borrow);
        settleRevenue(infoSet, price);
        return borrow.getId();
    }

    /**
     * 购买信息集（永久阅读权；一经售卖不可二次售卖，违者踢出知识宝库）
     */
    @Transactional
    public Long purchase(Long buyerUserId, Long infoSetId) {
        Adventurer buyer = requireNotKnowledgeBanned(buyerUserId);
        InfoSet infoSet = infoSetMapper.selectById(infoSetId);
        if (infoSet == null || infoSet.getStatus() != 1) {
            throw new BusinessException("信息集不存在或已下架");
        }
        Long bought = purchaseRecordMapper.selectCount(new LambdaQueryWrapper<PurchaseRecord>()
                .eq(PurchaseRecord::getBuyerId, buyerUserId)
                .eq(PurchaseRecord::getInfoSetId, infoSetId));
        if (bought > 0) {
            throw new BusinessException("已购买过该信息集");
        }
        financeService.deductGold(buyer.getId(), infoSet.getPrice(), "KNOWLEDGE_BUY", "INFO_SET", infoSetId);

        PurchaseRecord record = new PurchaseRecord();
        record.setInfoSetId(infoSetId);
        record.setBuyerId(buyerUserId);
        record.setPrice(infoSet.getPrice());
        record.setPurchasedAt(LocalDateTime.now());
        purchaseRecordMapper.insert(record);

        infoSetMapper.update(null, new LambdaUpdateWrapper<InfoSet>()
                .eq(InfoSet::getId, infoSetId)
                .setSql("sold_count = sold_count + 1"));
        settleRevenue(infoSet, infoSet.getPrice());
        return record.getId();
    }

    /**
     * 借阅信息集：1天卡(10%) / 周卡(25%) / 月卡(50%)，到期后失去阅读权
     */
    @Transactional
    public Long borrow(Long buyerUserId, Long infoSetId, int cardType) {
        if (cardType < CARD_DAY || cardType > CARD_MONTH) {
            throw new BusinessException("借阅卡类型非法");
        }
        Adventurer buyer = requireNotKnowledgeBanned(buyerUserId);
        InfoSet infoSet = infoSetMapper.selectById(infoSetId);
        if (infoSet == null || infoSet.getStatus() != 1) {
            throw new BusinessException("信息集不存在或已下架");
        }
        long price = Math.round(infoSet.getPrice() * CARD_RATE[cardType]);
        if (price <= 0) {
            price = 1;
        }
        financeService.deductGold(buyer.getId(), price, "KNOWLEDGE_BORROW", "INFO_SET", infoSetId);

        BorrowRecord record = new BorrowRecord();
        record.setInfoSetId(infoSetId);
        record.setUserId(buyerUserId);
        record.setCardType(cardType);
        record.setPrice(price);
        record.setStartAt(LocalDateTime.now());
        record.setExpireAt(LocalDateTime.now().plusDays(CARD_DAYS[cardType]));
        record.setCreatedAt(LocalDateTime.now());
        borrowRecordMapper.insert(record);
        settleRevenue(infoSet, price);
        return record.getId();
    }

    /**
     * 阅读信息集内容：需已购买，或持有未过期借阅。
     * 内容在 MinIO 时返回临时签名 URL（有效期不超过1小时且不超过借阅剩余时间），否则回退文本。
     */
    public ReadVO read(Long userId, Long infoSetId) {
        requireNotKnowledgeBanned(userId);
        InfoSet infoSet = infoSetMapper.selectById(infoSetId);
        if (infoSet == null) {
            throw new BusinessException("信息集不存在");
        }
        Long bought = purchaseRecordMapper.selectCount(new LambdaQueryWrapper<PurchaseRecord>()
                .eq(PurchaseRecord::getBuyerId, userId)
                .eq(PurchaseRecord::getInfoSetId, infoSetId));
        BorrowRecord borrow = borrowRecordMapper.selectOne(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getInfoSetId, infoSetId)
                .gt(BorrowRecord::getExpireAt, LocalDateTime.now())
                .orderByDesc(BorrowRecord::getExpireAt)
                .last("LIMIT 1"));
        boolean borrowed = borrow != null;
        if (bought == 0 && !borrowed) {
            throw new BusinessException(403, "请先购买或借阅该信息集");
        }

        String fallback = null;
        if (infoSet.getContentUrl() != null && !infoSet.getContentUrl().isBlank()) {
            // 借阅者签名有效期不超过借阅剩余时间，购买者固定1小时
            long ttl = 3600;
            if (bought == 0) {
                long remain = java.time.Duration.between(LocalDateTime.now(), borrow.getExpireAt()).getSeconds();
                ttl = Math.min(ttl, remain);
            }
            if (ttl > 0) {
                try {
                    return new ReadVO("url", minioService.presignedGetUrl(infoSet.getContentUrl(), (int) ttl));
                } catch (Exception e) {
                    log.warn("签名URL生成失败，回退文本: {}", e.getMessage());
                }
            }
        }
        KnowledgeContribution c = contributionMapper.selectById(infoSet.getContributionId());
        fallback = c == null ? infoSet.getSummary() : c.getContent();
        return new ReadVO("text", fallback);
    }

    /**
     * 版权违规处置：二次售卖/传播牟利 -> 踢出知识宝库
     */
    @Transactional
    public void reportViolation(Long handlerUserId, Long offenderUserId, Long evidenceRef) {
        ViolationRecord record = new ViolationRecord();
        record.setUserId(offenderUserId);
        record.setType(1);
        record.setPenalty(1);
        record.setHandlerId(handlerUserId);
        record.setCreatedAt(LocalDateTime.now());
        violationRecordMapper.insert(record);

        adventurerMapper.update(null, new LambdaUpdateWrapper<Adventurer>()
                .eq(Adventurer::getUserId, offenderUserId)
                .set(Adventurer::getKnowledgeBanned, 1));
        log.info("冒险者 {} 因二次售卖被踢出知识宝库（处理人 {}）", offenderUserId, handlerUserId);
    }

    private void settleRevenue(InfoSet infoSet, long revenue) {
        long toContributor = Math.round(revenue * CONTRIBUTOR_SHARE);
        String month = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Adventurer contributor = adventurerMapper.selectOne(
                new LambdaQueryWrapper<Adventurer>().eq(Adventurer::getUserId, infoSet.getContributorId()));
        if (contributor != null && toContributor > 0) {
            financeService.addGold(contributor.getId(), toContributor, "KNOWLEDGE_SHARE", "INFO_SET", infoSet.getId());
        }
        financeService.ledgerDelta(FinanceService.ASSOC_KNOWLEDGE, month, revenue - toContributor, 0);
    }

    private Adventurer requireNotKnowledgeBanned(Long userId) {
        Adventurer adv = adventurerMapper.selectOne(
                new LambdaQueryWrapper<Adventurer>().eq(Adventurer::getUserId, userId));
        if (adv == null) {
            throw new BusinessException("冒险者档案不存在");
        }
        if (adv.getKnowledgeBanned() != null && adv.getKnowledgeBanned() == 1) {
            throw new BusinessException(403, "已被知识宝库处置，永久失去进入知识宝库的权利");
        }
        return adv;
    }

    // ==================== 收藏 ====================

    /**
     * 收藏/取消收藏（幂等切换），返回 true=已收藏 false=已取消
     */
    @Transactional
    public boolean toggleFavorite(Long userId, Long infoSetId) {
        if (infoSetMapper.selectById(infoSetId) == null) {
            throw new BusinessException("信息集不存在");
        }
        com.bravetest.entity.KnowledgeFavorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<com.bravetest.entity.KnowledgeFavorite>()
                        .eq(com.bravetest.entity.KnowledgeFavorite::getUserId, userId)
                        .eq(com.bravetest.entity.KnowledgeFavorite::getInfoSetId, infoSetId));
        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return false;
        }
        com.bravetest.entity.KnowledgeFavorite fav = new com.bravetest.entity.KnowledgeFavorite();
        fav.setUserId(userId);
        fav.setInfoSetId(infoSetId);
        fav.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(fav);
        return true;
    }

    /**
     * 我的收藏（含信息集摘要）
     */
    public List<Map<String, Object>> myFavorites(Long userId) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.bravetest.entity.KnowledgeFavorite f : favoriteMapper.selectList(
                new LambdaQueryWrapper<com.bravetest.entity.KnowledgeFavorite>()
                        .eq(com.bravetest.entity.KnowledgeFavorite::getUserId, userId)
                        .orderByDesc(com.bravetest.entity.KnowledgeFavorite::getCreatedAt))) {
            InfoSet is = infoSetMapper.selectById(f.getInfoSetId());
            if (is == null) continue;
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("infoSetId", is.getId());
            row.put("title", is.getTitle());
            row.put("summary", is.getSummary());
            row.put("price", is.getPrice());
            row.put("status", is.getStatus());
            row.put("favoritedAt", f.getCreatedAt());
            result.add(row);
        }
        return result;
    }

    /** 我收藏的信息集 id 集合（供列表页标记 ♡） */
    public List<Long> myFavoriteIds(Long userId) {
        return favoriteMapper.selectList(new LambdaQueryWrapper<com.bravetest.entity.KnowledgeFavorite>()
                        .eq(com.bravetest.entity.KnowledgeFavorite::getUserId, userId))
                .stream().map(com.bravetest.entity.KnowledgeFavorite::getInfoSetId).toList();
    }

    // ==================== 举报 ====================

    /**
     * 用户提交版权违规举报（不可举报自己）
     */
    public Long submitReport(Long reporterUserId, Long offenderUserId, Long infoSetId, String description) {
        if (reporterUserId.equals(offenderUserId)) {
            throw new BusinessException("不能举报自己");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException("请填写举报说明");
        }
        com.bravetest.entity.ViolationReport report = new com.bravetest.entity.ViolationReport();
        report.setReporterId(reporterUserId);
        report.setOffenderId(offenderUserId);
        report.setInfoSetId(infoSetId);
        report.setDescription(description);
        report.setStatus(com.bravetest.entity.ViolationReport.ST_PENDING);
        report.setCreatedAt(LocalDateTime.now());
        violationReportMapper.insert(report);
        return report.getId();
    }

    /** 待处理举报列表 */
    public List<com.bravetest.entity.ViolationReport> listPendingReports() {
        return violationReportMapper.selectList(
                new LambdaQueryWrapper<com.bravetest.entity.ViolationReport>()
                        .eq(com.bravetest.entity.ViolationReport::getStatus,
                                com.bravetest.entity.ViolationReport.ST_PENDING)
                        .orderByAsc(com.bravetest.entity.ViolationReport::getCreatedAt));
    }

    /**
     * 管理员处理举报：penalize=true 踢出知识宝库（复用 reportViolation），否则驳回
     */
    @Transactional
    public void handleReport(Long adminUserId, Long reportId, boolean penalize, String remark) {
        com.bravetest.entity.ViolationReport report = violationReportMapper.selectById(reportId);
        if (report == null || report.getStatus() != com.bravetest.entity.ViolationReport.ST_PENDING) {
            throw new BusinessException("举报不存在或已处理");
        }
        if (penalize) {
            reportViolation(adminUserId, report.getOffenderId(), report.getInfoSetId());
        }
        report.setStatus(penalize ? com.bravetest.entity.ViolationReport.ST_PENALIZED
                : com.bravetest.entity.ViolationReport.ST_DISMISSED);
        report.setHandlerId(adminUserId);
        report.setHandleRemark(remark);
        violationReportMapper.updateById(report);
    }
}
