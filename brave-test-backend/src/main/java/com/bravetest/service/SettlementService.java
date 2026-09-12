package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.entity.AssociationLedger;
import com.bravetest.entity.SettlementLog;
import com.bravetest.mapper.AssociationLedgerMapper;
import com.bravetest.mapper.SettlementLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 月度结算服务（每月1日 00:30 自动执行，可手动触发）：
 * 1. 冒险者协会上月佣金 60% 上缴总管理员
 * 2. 总管理员上月收到营利的 40% 拨给知识宝库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AssociationLedgerMapper ledgerMapper;
    private final SettlementLogMapper settlementLogMapper;
    private final FinanceService financeService;

    @Scheduled(cron = "0 30 0 1 * ?")
    public void scheduledSettle() {
        String lastMonth = LocalDateTime.now().minusMonths(1).format(MONTH_FMT);
        settle(lastMonth);
    }

    /**
     * 对指定月份执行结算（幂等：settlement_log 按 month+type 唯一）
     */
    @Transactional
    public void settle(String month) {
        // ---- 结算1：冒险者协会佣金 60% 上缴 ----
        if (settlementLogMapper.selectCount(new LambdaQueryWrapper<SettlementLog>()
                .eq(SettlementLog::getMonth, month)
                .eq(SettlementLog::getType, 1)) == 0) {
            long commissionIncome = getIncome(FinanceService.ASSOC_ADVENTURER, month);
            long toSuper = Math.round(commissionIncome * 0.60);
            if (toSuper > 0) {
                financeService.ledgerDelta(FinanceService.ASSOC_ADVENTURER, month, 0, toSuper);
                financeService.ledgerDelta(FinanceService.ASSOC_SUPER, month, toSuper, 0);
                saveLog(month, 1, toSuper, FinanceService.ASSOC_ADVENTURER, FinanceService.ASSOC_SUPER);
                log.info("[月度结算] {} 冒险者协会佣金 {} 的60%={} 已上缴总管理员", month, commissionIncome, toSuper);
            }
        }

        // ---- 结算2：总管理员收到营利的 40% 拨给知识宝库 ----
        if (settlementLogMapper.selectCount(new LambdaQueryWrapper<SettlementLog>()
                .eq(SettlementLog::getMonth, month)
                .eq(SettlementLog::getType, 2)) == 0) {
            long superIncome = getIncome(FinanceService.ASSOC_SUPER, month);
            long toKnowledge = Math.round(superIncome * 0.40);
            if (toKnowledge > 0) {
                financeService.ledgerDelta(FinanceService.ASSOC_SUPER, month, 0, toKnowledge);
                financeService.ledgerDelta(FinanceService.ASSOC_KNOWLEDGE, month, toKnowledge, 0);
                saveLog(month, 2, toKnowledge, FinanceService.ASSOC_SUPER, FinanceService.ASSOC_KNOWLEDGE);
                log.info("[月度结算] {} 总管理员营利 {} 的40%={} 已拨付知识宝库", month, superIncome, toKnowledge);
            }
        }
    }

    private long getIncome(int association, String month) {
        AssociationLedger ledger = ledgerMapper.selectOne(new LambdaQueryWrapper<AssociationLedger>()
                .eq(AssociationLedger::getAssociation, association)
                .eq(AssociationLedger::getMonth, month));
        return ledger == null ? 0 : ledger.getIncome();
    }

    private void saveLog(String month, int type, long amount, int from, int to) {
        SettlementLog logEntry = new SettlementLog();
        logEntry.setMonth(month);
        logEntry.setType(type);
        logEntry.setAmount(amount);
        logEntry.setFromAccount(from);
        logEntry.setToAccount(to);
        logEntry.setCreatedAt(LocalDateTime.now());
        settlementLogMapper.insert(logEntry);
    }
}
