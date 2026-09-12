package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.AssociationLedger;
import com.bravetest.entity.GoldFlow;
import com.bravetest.mapper.AdventurerMapper;
import com.bravetest.mapper.AssociationLedgerMapper;
import com.bravetest.mapper.GoldFlowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 金币与协会账本财务服务（乐观扣款，防并发超支）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceService {

    public static final int ASSOC_XINGZHE = 1;
    public static final int ASSOC_ADVENTURER = 2;
    public static final int ASSOC_KNOWLEDGE = 3;
    public static final int ASSOC_SUPER = 4;

    private final AdventurerMapper adventurerMapper;
    private final GoldFlowMapper goldFlowMapper;
    private final AssociationLedgerMapper ledgerMapper;

    public Adventurer requireAdventurerByUserId(Long userId) {
        Adventurer adv = adventurerMapper.selectOne(
                new LambdaQueryWrapper<Adventurer>().eq(Adventurer::getUserId, userId));
        if (adv == null) {
            throw new BusinessException("冒险者档案不存在");
        }
        return adv;
    }

    /**
     * 扣减金币：UPDATE ... SET balance = balance - ? WHERE balance >= ? 保证不超支
     */
    @Transactional
    public void deductGold(Long adventurerId, long amount, String bizType, String refType, Long refId) {
        if (amount <= 0) {
            throw new BusinessException("扣款金额必须大于0");
        }
        int rows = adventurerMapper.update(null, new LambdaUpdateWrapper<Adventurer>()
                .eq(Adventurer::getId, adventurerId)
                .ge(Adventurer::getGoldBalance, amount)
                .setSql("gold_balance = gold_balance - " + amount));
        if (rows == 0) {
            throw new BusinessException("金币不足");
        }
        Adventurer adv = adventurerMapper.selectById(adventurerId);
        saveFlow(adv.getUserId(), -amount, adv.getGoldBalance(), bizType, refType, refId);
    }

    /**
     * 增加金币
     */
    @Transactional
    public void addGold(Long adventurerId, long amount, String bizType, String refType, Long refId) {
        if (amount <= 0) {
            throw new BusinessException("入账金额必须大于0");
        }
        adventurerMapper.update(null, new LambdaUpdateWrapper<Adventurer>()
                .eq(Adventurer::getId, adventurerId)
                .setSql("gold_balance = gold_balance + " + amount));
        Adventurer adv = adventurerMapper.selectById(adventurerId);
        saveFlow(adv.getUserId(), amount, adv.getGoldBalance(), bizType, refType, refId);
    }

    private void saveFlow(Long userId, long amount, long balanceAfter, String bizType, String refType, Long refId) {
        GoldFlow flow = new GoldFlow();
        flow.setUserId(userId);
        flow.setBizType(bizType);
        flow.setAmount(amount);
        flow.setBalanceAfter(balanceAfter);
        flow.setRefType(refType);
        flow.setRefId(refId);
        flow.setCreatedAt(LocalDateTime.now());
        goldFlowMapper.insert(flow);
    }

    /**
     * 协会账本入账（income/expense 正负增量），不存在则初始化当月账本
     */
    @Transactional
    public void ledgerDelta(int association, String month, long incomeDelta, long expenseDelta) {
        AssociationLedger ledger = ledgerMapper.selectOne(new LambdaQueryWrapper<AssociationLedger>()
                .eq(AssociationLedger::getAssociation, association)
                .eq(AssociationLedger::getMonth, month));
        if (ledger == null) {
            ledger = new AssociationLedger();
            ledger.setAssociation(association);
            ledger.setMonth(month);
            ledger.setIncome(Math.max(incomeDelta, 0));
            ledger.setExpense(Math.max(expenseDelta, 0));
            ledger.setBalance(incomeDelta - expenseDelta);
            ledger.setUpdatedAt(LocalDateTime.now());
            ledgerMapper.insert(ledger);
        } else {
            ledger.setIncome(ledger.getIncome() + incomeDelta);
            ledger.setExpense(ledger.getExpense() + expenseDelta);
            ledger.setBalance(ledger.getBalance() + incomeDelta - expenseDelta);
            ledger.setUpdatedAt(LocalDateTime.now());
            ledgerMapper.updateById(ledger);
        }
    }
}
