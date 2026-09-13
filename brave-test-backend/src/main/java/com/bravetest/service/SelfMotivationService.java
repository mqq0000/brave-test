package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.GoldFlow;
import com.bravetest.entity.SelfRedemption;
import com.bravetest.entity.SelfShopItem;
import com.bravetest.entity.SelfTask;
import com.bravetest.entity.Skill;
import com.bravetest.mapper.GoldFlowMapper;
import com.bravetest.mapper.SelfRedemptionMapper;
import com.bravetest.mapper.SelfShopItemMapper;
import com.bravetest.mapper.SelfTaskMapper;
import com.bravetest.mapper.SkillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 修身·自我激励系统：
 * <ul>
 *   <li>自我任务：自己给自己发任务，完成得虚拟币（自我铸币每日上限）+ 关联技能经验 + 净化萌芽</li>
 *   <li>技能树：树形结构，每 {@value Skill#EXP_PER_LEVEL} 经验升 1 级；开枝（建子技能）费用全额入冒险者协会</li>
 *   <li>自我商城：自建现实奖励商品，兑换扣币，10% 会费入冒险者协会账本（与佣金体系接轨）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelfMotivationService {

    /** 每日自我铸币上限（防止奖励通胀） */
    public static final long DAILY_MINT_CAP = 500;
    /** 每次完成任务获得的技能经验 */
    public static final int EXP_PER_COMPLETION = 20;
    /** 连续打卡：每多连续 1 天额外奖励 +5 币，封顶 +50 */
    public static final int STREAK_BONUS_PER_DAY = 5;
    public static final int STREAK_BONUS_CAP = 50;
    /** 每日边界与打卡日期统一按东八区计算 */
    private static final java.time.ZoneId ZONE = java.time.ZoneId.of("Asia/Shanghai");
    /** 兑换金额入冒险者协会的比例 */
    private static final double ASSOC_FEE_RATE = 0.10;
    /** 默认技能树模板 */
    private static final Map<String, List<String>> DEFAULT_TREE = Map.of(
            "专注", List.of("深度工作", "冥想"),
            "健体", List.of("跑步", "力量训练"),
            "心性", List.of("阅读", "日记"));

    private final SelfTaskMapper taskMapper;
    private final SkillMapper skillMapper;
    private final SelfShopItemMapper itemMapper;
    private final SelfRedemptionMapper redemptionMapper;
    private final GoldFlowMapper goldFlowMapper;
    private final FinanceService financeService;
    private final AdventurerService adventurerService;

    // ==================== 自我任务 ====================

    public List<SelfTask> listTasks(Long userId) {
        return taskMapper.selectList(new LambdaQueryWrapper<SelfTask>()
                .eq(SelfTask::getUserId, userId)
                .orderByAsc(SelfTask::getStatus)
                .orderByDesc(SelfTask::getCreatedAt));
    }

    @Transactional
    public Long createTask(Long userId, String title, String description,
                           int coinReward, Long skillId, int repeatType) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("请填写任务标题");
        }
        if (coinReward < 1 || coinReward > 100) {
            throw new BusinessException("单次奖励需在 1~100 金币之间");
        }
        if (repeatType != SelfTask.REPEAT_ONCE && repeatType != SelfTask.REPEAT_DAILY) {
            throw new BusinessException("任务类型非法");
        }
        if (skillId != null && skillMapper.selectOne(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getId, skillId).eq(Skill::getUserId, userId)) == null) {
            throw new BusinessException("关联技能不存在");
        }
        SelfTask task = new SelfTask();
        task.setUserId(userId);
        task.setTitle(title.trim());
        task.setDescription(description);
        task.setCoinReward(coinReward);
        task.setSkillId(skillId);
        task.setRepeatType(repeatType);
        task.setStatus(SelfTask.ST_ACTIVE);
        task.setCreatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return task.getId();
    }

    /**
     * 完成自我任务：发币（受每日铸币上限约束）+ 技能经验 + 净化萌芽
     */
    @Transactional
    public Map<String, Object> completeTask(Long userId, Long taskId) {
        SelfTask task = requireOwnTask(userId, taskId);
        if (task.getStatus() != SelfTask.ST_ACTIVE) {
            throw new BusinessException("该任务已完结");
        }
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDate today = now.toLocalDate();
        if (task.getRepeatType() == SelfTask.REPEAT_DAILY
                && task.getLastCompletedAt() != null
                && task.getLastCompletedAt().toLocalDate().equals(today)) {
            throw new BusinessException("每日任务今日已完成，明天再来");
        }

        // 连续打卡：昨天完成过则 +1，否则重置为第 1 天（仅每日任务）
        int streakDays = 1;
        int streakBonus = 0;
        if (task.getRepeatType() == SelfTask.REPEAT_DAILY) {
            if (task.getLastCompletedAt() != null
                    && task.getLastCompletedAt().toLocalDate().equals(today.minusDays(1))) {
                streakDays = (task.getStreakDays() == null ? 0 : task.getStreakDays()) + 1;
            }
            streakBonus = Math.min((streakDays - 1) * STREAK_BONUS_PER_DAY, STREAK_BONUS_CAP);
        }

        long totalReward = task.getCoinReward() + streakBonus;
        long mintedToday = mintedToday(userId);
        if (mintedToday + totalReward > DAILY_MINT_CAP) {
            throw new BusinessException("今日自我激励奖励已达上限 " + DAILY_MINT_CAP + " 金币，休息一下，明天继续");
        }

        Adventurer adv = financeService.requireAdventurerByUserId(userId);
        financeService.addGold(adv.getId(), totalReward, "SELF_TASK", "SELF_TASK", taskId);

        Map<String, Object> vo = new HashMap<>();
        vo.put("reward", totalReward);
        vo.put("baseReward", task.getCoinReward());
        vo.put("streakBonus", streakBonus);

        // 技能经验与升级
        if (task.getSkillId() != null) {
            Skill skill = skillMapper.selectById(task.getSkillId());
            if (skill != null && userId.equals(skill.getUserId())) {
                int oldLevel = skill.level();
                int newExp = skill.getExp() + EXP_PER_COMPLETION;
                skillMapper.update(null, new LambdaUpdateWrapper<Skill>()
                        .eq(Skill::getId, skill.getId())
                        .set(Skill::getExp, newExp));
                skill.setExp(newExp);
                vo.put("skillName", skill.getName());
                vo.put("skillExp", newExp);
                vo.put("skillLevel", skill.level());
                vo.put("levelUp", skill.level() > oldLevel);
            }
        }

        // 修身养性：完成自我承诺即净化萌芽
        adventurerService.purify(adv.getId(), 2, "SELF_TASK", taskId);

        SelfTask update = new SelfTask();
        update.setId(task.getId());
        update.setLastCompletedAt(now);
        update.setStatus(task.getRepeatType() == SelfTask.REPEAT_DAILY
                ? SelfTask.ST_ACTIVE : SelfTask.ST_DONE);
        if (task.getRepeatType() == SelfTask.REPEAT_DAILY) {
            update.setStreakDays(streakDays);
        }
        taskMapper.updateById(update);

        vo.put("streakDays", streakDays);
        vo.put("mintedToday", mintedToday + totalReward);
        vo.put("dailyCap", DAILY_MINT_CAP);
        return vo;
    }

    @Transactional
    public void abandonTask(Long userId, Long taskId) {
        SelfTask task = requireOwnTask(userId, taskId);
        if (task.getStatus() != SelfTask.ST_ACTIVE) {
            throw new BusinessException("该任务已完结");
        }
        SelfTask update = new SelfTask();
        update.setId(task.getId());
        update.setStatus(SelfTask.ST_ABANDONED);
        taskMapper.updateById(update);
    }

    private SelfTask requireOwnTask(Long userId, Long taskId) {
        SelfTask task = taskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(403, "任务不存在或无权操作");
        }
        return task;
    }

    /** 今日自我铸币总额（金币流水 SELF_TASK 入账之和） */
    private long mintedToday(Long userId) {
        List<GoldFlow> flows = goldFlowMapper.selectList(new LambdaQueryWrapper<GoldFlow>()
                .eq(GoldFlow::getUserId, userId)
                .eq(GoldFlow::getBizType, "SELF_TASK")
                .gt(GoldFlow::getAmount, 0)
                .ge(GoldFlow::getCreatedAt, LocalDate.now(ZONE).atStartOfDay()));
        return flows.stream().mapToLong(f -> f.getAmount() == null ? 0 : f.getAmount()).sum();
    }

    // ==================== 技能树 ====================

    /** 技能树（含推导等级） */
    public List<Map<String, Object>> skillTree(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Skill s : skillMapper.selectList(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getUserId, userId)
                .orderByAsc(Skill::getCreatedAt))) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", s.getId());
            row.put("parentId", s.getParentId());
            row.put("name", s.getName());
            row.put("exp", s.getExp());
            row.put("level", s.level());
            row.put("nextLevelExp", Skill.MAX_LEVEL == s.level()
                    ? null : s.level() * Skill.EXP_PER_LEVEL);
            result.add(row);
        }
        return result;
    }

    /** 初始化默认技能树（专注/健体/心性 三系各带子技能） */
    @Transactional
    public int initSkills(Long userId) {
        Long count = skillMapper.selectCount(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getUserId, userId));
        if (count != null && count > 0) {
            throw new BusinessException("技能树已存在，无需重复初始化");
        }
        int created = 0;
        for (Map.Entry<String, List<String>> root : DEFAULT_TREE.entrySet()) {
            Skill r = insertSkill(userId, null, root.getKey());
            created++;
            for (String child : root.getValue()) {
                insertSkill(userId, r.getId(), child);
                created++;
            }
        }
        return created;
    }

    /** 开枝：在指定节点下创建子技能，费用全额入冒险者协会账本 */
    @Transactional
    public Long branchSkill(Long userId, Long parentId, String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("请填写技能名称");
        }
        Skill parent = skillMapper.selectOne(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getId, parentId).eq(Skill::getUserId, userId));
        if (parent == null) {
            throw new BusinessException("父技能不存在");
        }
        Adventurer adv = financeService.requireAdventurerByUserId(userId);
        financeService.deductGold(adv.getId(), Skill.BRANCH_COST, "SKILL_BRANCH", "SKILL", parentId);
        String month = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        financeService.ledgerDelta(FinanceService.ASSOC_ADVENTURER, month, Skill.BRANCH_COST, 0);
        return insertSkill(userId, parentId, name.trim()).getId();
    }

    private Skill insertSkill(Long userId, Long parentId, String name) {
        Skill skill = new Skill();
        skill.setUserId(userId);
        skill.setParentId(parentId);
        skill.setName(name);
        skill.setExp(0);
        skill.setCreatedAt(LocalDateTime.now());
        skillMapper.insert(skill);
        return skill;
    }

    // ==================== 自我奖励商城 ====================

    public List<SelfShopItem> listItems(Long userId) {
        return itemMapper.selectList(new LambdaQueryWrapper<SelfShopItem>()
                .eq(SelfShopItem::getUserId, userId)
                .orderByAsc(SelfShopItem::getStatus)
                .orderByDesc(SelfShopItem::getCreatedAt));
    }

    @Transactional
    public Long addItem(Long userId, String name, String description, int cost) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("请填写奖励名称");
        }
        if (cost < 1 || cost > 100000) {
            throw new BusinessException("兑换价需在 1~100000 金币之间");
        }
        SelfShopItem item = new SelfShopItem();
        item.setUserId(userId);
        item.setName(name.trim());
        item.setDescription(description);
        item.setCost(cost);
        item.setStatus(SelfShopItem.ST_ON);
        item.setCreatedAt(LocalDateTime.now());
        itemMapper.insert(item);
        return item.getId();
    }

    @Transactional
    public void offShelfItem(Long userId, Long itemId) {
        SelfShopItem item = itemMapper.selectById(itemId);
        if (item == null || !userId.equals(item.getUserId())) {
            throw new BusinessException(403, "商品不存在或无权操作");
        }
        item.setStatus(SelfShopItem.ST_OFF);
        itemMapper.updateById(item);
    }

    /**
     * 兑换自我奖励：扣币 + 10% 会费入冒险者协会账本（接入协会经济体系）
     */
    @Transactional
    public SelfRedemption redeem(Long userId, Long itemId) {
        SelfShopItem item = itemMapper.selectById(itemId);
        if (item == null || !userId.equals(item.getUserId())
                || item.getStatus() != SelfShopItem.ST_ON) {
            throw new BusinessException("商品不存在或已下架");
        }
        Adventurer adv = financeService.requireAdventurerByUserId(userId);
        financeService.deductGold(adv.getId(), item.getCost(), "SELF_REDEEM", "SELF_ITEM", itemId);
        long fee = Math.round(item.getCost() * ASSOC_FEE_RATE);
        if (fee > 0) {
            String month = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            financeService.ledgerDelta(FinanceService.ASSOC_ADVENTURER, month, fee, 0);
        }
        SelfRedemption redemption = new SelfRedemption();
        redemption.setUserId(userId);
        redemption.setItemId(itemId);
        redemption.setItemName(item.getName());
        redemption.setCost(item.getCost());
        redemption.setAssocFee((int) fee);
        redemption.setRedeemedAt(LocalDateTime.now());
        redemptionMapper.insert(redemption);
        log.info("冒险者(userId={}) 兑换自我奖励「{}」，花费 {}，协会会费 {}", userId, item.getName(), item.getCost(), fee);
        return redemption;
    }

    public List<SelfRedemption> listRedemptions(Long userId) {
        return redemptionMapper.selectList(new LambdaQueryWrapper<SelfRedemption>()
                .eq(SelfRedemption::getUserId, userId)
                .orderByDesc(SelfRedemption::getRedeemedAt)
                .last("LIMIT 50"));
    }
}
