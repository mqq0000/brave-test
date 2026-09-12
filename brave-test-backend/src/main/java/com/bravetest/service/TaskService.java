package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.Task;
import com.bravetest.entity.TaskAcceptRecord;
import com.bravetest.entity.XingzheApply;
import com.bravetest.mapper.AdventurerMapper;
import com.bravetest.mapper.TaskAcceptRecordMapper;
import com.bravetest.mapper.TaskMapper;
import com.bravetest.mapper.XingzheApplyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 任务中心核心服务：发布 / 接单 / 提交 / 验收 / 升降级 / 封禁 / 行者协会审批
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    /** 任务来源 */
    public static final int SOURCE_XINGZHE = 1;
    public static final int SOURCE_ADVENTURER = 2;

    /** 任务状态 */
    public static final int ST_PENDING_REVIEW = 0;
    public static final int ST_OPEN = 1;
    public static final int ST_IN_PROGRESS = 2;
    public static final int ST_TO_VERIFY = 3;
    public static final int ST_COMPLETED = 4;
    public static final int ST_FAILED = 5;
    public static final int ST_REJECTED = 6;
    public static final int ST_OFFLINE = 7;

    private static final String[] LEVELS = {"D", "C", "B", "A", "S"};
    /** 各等级报酬区间下限 */
    private static final long[] LEVEL_MIN = {80, 800, 8000, 80000, 120000};
    /** 各等级报酬区间上限（S级上不封顶用 Long.MAX） */
    private static final long[] LEVEL_MAX = {120, 1200, 12000, 120000, Long.MAX_VALUE};

    /** 每完成 N 件自然升级 */
    private static final int TASKS_PER_LEVEL = 100;
    /** 升级奖励金币 */
    private static final long LEVEL_UP_REWARD = 10000;
    /** 佣金比例：协会抽成10%，接取者得90% */
    private static final double COMMISSION_RATE = 0.10;
    /** 越级失败封禁天数 */
    private static final int BAN_DAYS = 30;
    /** 允许越级的最大级差 */
    private static final int MAX_CROSS_LEVEL_GAP = 2;

    private static final String ACCEPT_LOCK_PREFIX = "lock:task:accept:";

    private final TaskMapper taskMapper;
    private final TaskAcceptRecordMapper acceptRecordMapper;
    private final AdventurerMapper adventurerMapper;
    private final XingzheApplyMapper xingzheApplyMapper;
    private final FinanceService financeService;
    private final AdventurerService adventurerService;
    private final StringRedisTemplate redisTemplate;

    public static int levelIndex(String level) {
        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i].equalsIgnoreCase(level)) {
                return i;
            }
        }
        throw new BusinessException("非法任务等级：" + level);
    }

    private void validateLevelRange(String level, long reward) {
        int idx = levelIndex(level);
        if (reward < LEVEL_MIN[idx] || reward > LEVEL_MAX[idx]) {
            throw new BusinessException(level + " 级任务报酬须在 "
                    + LEVEL_MIN[idx] + "~" + (LEVEL_MAX[idx] == Long.MAX_VALUE ? "不限" : LEVEL_MAX[idx]) + " 金币之间");
        }
    }

    // ==================== 冒险者协会：发布 ====================

    /**
     * 冒险者发布任务：预付托管全额金币，协会抽成10%
     */
    @Transactional
    public Long publishAdventurerTask(Long userId, String title, String description, String taskLevel, Long rewardGold) {
        Adventurer adv = financeService.requireAdventurerByUserId(userId);
        checkNotBanned(adv);
        validateLevelRange(taskLevel, rewardGold);

        Task task = new Task();
        task.setSourceType(SOURCE_ADVENTURER);
        task.setPublisherId(userId);
        task.setTitle(title);
        task.setDescription(description);
        task.setTaskLevel(taskLevel.toUpperCase());
        task.setRewardGold(rewardGold);
        task.setAcceptorGold(Math.round(rewardGold * (1 - COMMISSION_RATE)));
        task.setPurifyValue(2);
        task.setStatus(ST_PENDING_REVIEW);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        // 托管发布者报酬
        financeService.deductGold(adv.getId(), rewardGold, "ESCROW", "TASK", task.getId());
        return task.getId();
    }

    // ==================== 行者协会 ====================

    /**
     * 总管理员发布善事任务（救济、资助等无条件任务）
     */
    @Transactional
    public Long publishXingzheTaskBySuper(Long superUserId, String title, String description,
                                          String taskLevel, Long rewardGold, int purifyValue) {
        validateLevelRange(taskLevel, rewardGold);
        Task task = new Task();
        task.setSourceType(SOURCE_XINGZHE);
        task.setPublisherId(superUserId);
        task.setTitle(title);
        task.setDescription(description);
        task.setTaskLevel(taskLevel.toUpperCase());
        task.setRewardGold(rewardGold == null ? 0 : rewardGold);
        task.setAcceptorGold(rewardGold == null ? 0 : Math.round(rewardGold * (1 - COMMISSION_RATE)));
        task.setPurifyValue(purifyValue);
        task.setStatus(ST_OPEN);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return task.getId();
    }

    /**
     * 冒险者向行者协会提交任务帮助申请
     */
    public Long submitXingzheApply(Long userId, String title, String content) {
        XingzheApply apply = new XingzheApply();
        apply.setApplicantId(userId);
        apply.setTitle(title);
        apply.setContent(content);
        apply.setAuditStatus(0);
        apply.setCreatedAt(LocalDateTime.now());
        xingzheApplyMapper.insert(apply);
        return apply.getId();
    }

    /**
     * 行者协会管理员审批：通过后自动发布为行者任务
     */
    @Transactional
    public void auditXingzheApply(Long adminUserId, Long applyId, boolean pass, String remark) {
        XingzheApply apply = xingzheApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException("申请不存在");
        }
        if (apply.getAuditStatus() != 0) {
            throw new BusinessException("该申请已审批");
        }
        apply.setAuditStatus(pass ? 1 : 2);
        apply.setAuditorId(adminUserId);
        apply.setAuditRemark(remark);
        xingzheApplyMapper.updateById(apply);
        if (pass) {
            Long taskId = publishXingzheTaskBySuper(adminUserId, apply.getTitle(),
                    apply.getContent(), "D", 100L, 10);
            apply.setPublishedTaskId(taskId);
            xingzheApplyMapper.updateById(apply);
        }
    }

    // ==================== 接单 / 提交 / 验收 ====================

    /**
     * 接单：Redis 分布式锁防超抢 + 等级/封禁/赤化校验 + 乐观占单
     * <p>注意：锁在事务提交前释放存在极小竞态窗口，但乐观占单（status=1 条件更新）保证了不会重复接单。
     *
     * @param confirmCrossLevel 越级接单需显式确认
     */
    @Transactional
    public Long acceptTask(Long userId, Long taskId, boolean confirmCrossLevel) {
        String lockKey = ACCEPT_LOCK_PREFIX + taskId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException("任务正在被其他冒险者接取，请稍后重试");
        }
        try {
            Adventurer adv = financeService.requireAdventurerByUserId(userId);
            checkNotBanned(adv);

            Task task = taskMapper.selectById(taskId);
            if (task == null) {
                throw new BusinessException("任务不存在");
            }
            if (task.getStatus() != ST_OPEN) {
                throw new BusinessException("任务当前不可接取");
            }
            // 赤化状态禁止接取 S 级任务
            if ("RED".equals(adv.getSproutStatus()) && "S".equalsIgnoreCase(task.getTaskLevel())) {
                throw new BusinessException("萌芽赤化，无法接取 S 级任务，先去净化萌芽吧");
            }

            int ownIdx = levelIndex(adv.getRankLevel());
            int taskIdx = levelIndex(task.getTaskLevel());
            boolean crossLevel = taskIdx > ownIdx;
            if (crossLevel) {
                if (taskIdx - ownIdx > MAX_CROSS_LEVEL_GAP) {
                    throw new BusinessException("最多允许越级挑战 " + MAX_CROSS_LEVEL_GAP + " 级的任务");
                }
                if (!confirmCrossLevel) {
                    throw new BusinessException(400, "越级接单风险极大，失败将被协会暂停开放一个月，请传 confirmCrossLevel=true 确认");
                }
            }

            // 乐观占单：仅当任务仍处于发布中才允许接取
            int rows = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                    .eq(Task::getId, taskId)
                    .eq(Task::getStatus, ST_OPEN)
                    .set(Task::getStatus, ST_IN_PROGRESS)
                    .set(Task::getAcceptorId, userId)
                    .set(Task::getUpdatedAt, LocalDateTime.now()));
            if (rows == 0) {
                throw new BusinessException("手慢了，任务已被其他冒险者接取");
            }

            TaskAcceptRecord record = new TaskAcceptRecord();
            record.setTaskId(taskId);
            record.setAdventurerId(adv.getId());
            record.setAdventurerUserId(userId);
            record.setIsCrossLevel(crossLevel ? 1 : 0);
            record.setStatus(0);
            record.setStartedAt(LocalDateTime.now());
            acceptRecordMapper.insert(record);
            return record.getId();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 冒险者提交成果 -> 任务进入待验收
     */
    @Transactional
    public void submitTask(Long userId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getAcceptorId())) {
            throw new BusinessException("无权操作该任务");
        }
        if (task.getStatus() != ST_IN_PROGRESS) {
            throw new BusinessException("任务当前状态不可提交");
        }
        task.setStatus(ST_TO_VERIFY);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    /**
     * 验收：成功 -> 结算 + 净化 + 升级判定；失败 -> 越级封禁一个月 + 退还发布者90%
     */
    @Transactional
    public void verifyTask(Long operatorUserId, Long taskId, boolean success, boolean byAdmin) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!byAdmin && !operatorUserId.equals(task.getPublisherId())) {
            throw new BusinessException(403, "只有发布者或管理员可验收");
        }
        if (task.getStatus() != ST_TO_VERIFY) {
            throw new BusinessException("任务不在待验收状态");
        }
        TaskAcceptRecord record = acceptRecordMapper.selectOne(new LambdaQueryWrapper<TaskAcceptRecord>()
                .eq(TaskAcceptRecord::getTaskId, taskId)
                .eq(TaskAcceptRecord::getStatus, 0)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException("接取记录异常");
        }
        Adventurer acceptor = adventurerMapper.selectById(record.getAdventurerId());

        if (success) {
            // 1. 发放接取者90%报酬
            if (task.getAcceptorGold() != null && task.getAcceptorGold() > 0) {
                financeService.addGold(acceptor.getId(), task.getAcceptorGold(), "TASK_REWARD", "TASK", taskId);
            }
            // 2. 净化萌芽
            int purify = task.getPurifyValue() == null ? 0 : task.getPurifyValue();
            if (purify > 0) {
                adventurerService.purify(acceptor.getId(), purify, "XINGZHE_TASK", taskId);
            }
            // 3. 完成计数 + 升级判定
            int newCount = (acceptor.getCompletedCount() == null ? 0 : acceptor.getCompletedCount()) + 1;
            String ownLevel = acceptor.getRankLevel();
            int ownIdx = levelIndex(ownLevel);
            int taskIdx = levelIndex(task.getTaskLevel());
            String newLevel = ownLevel;
            boolean promoted = false;

            if (record.getIsCrossLevel() == 1 && taskIdx > ownIdx) {
                // 越级成功：直升至该任务等级
                newLevel = LEVELS[taskIdx];
                promoted = true;
            } else if (newCount % TASKS_PER_LEVEL == 0 && ownIdx < LEVELS.length - 1) {
                // 每100件自然升级
                newLevel = LEVELS[ownIdx + 1];
                promoted = true;
            }
            // 越级与自然升级同时满足时取更高等级，奖励只发一次
            if (promoted && levelIndex(newLevel) > ownIdx) {
                adventurerMapper.update(null, new LambdaUpdateWrapper<Adventurer>()
                        .eq(Adventurer::getId, acceptor.getId())
                        .set(Adventurer::getRankLevel, newLevel));
                financeService.addGold(acceptor.getId(), LEVEL_UP_REWARD, "LEVEL_UP_REWARD", "ADVENTURER", acceptor.getId());
                financeService.ledgerDelta(FinanceService.ASSOC_ADVENTURER,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        0, LEVEL_UP_REWARD);
            }
            adventurerMapper.update(null, new LambdaUpdateWrapper<Adventurer>()
                    .eq(Adventurer::getId, acceptor.getId())
                    .set(Adventurer::getCompletedCount, newCount));
            // 更新 Redis 完成任务数排行榜
            adventurerService.recordRank(acceptor.getUserId(), newCount);

            // 4. 佣金入协会账本
            long commission = task.getRewardGold() - task.getAcceptorGold();
            if (commission > 0) {
                financeService.ledgerDelta(FinanceService.ASSOC_ADVENTURER,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        commission, 0);
            }
            finish(task, record, ST_COMPLETED, 1, task.getAcceptorGold(), purify);
        } else {
            // 失败：越级失败封禁一个月；退还发布者托管报酬的90%（佣金不退）
            if (record.getIsCrossLevel() == 1) {
                LocalDateTime until = LocalDateTime.now().plusDays(BAN_DAYS);
                adventurerMapper.update(null, new LambdaUpdateWrapper<Adventurer>()
                        .eq(Adventurer::getId, acceptor.getId())
                        .set(Adventurer::getBannedUntil, until));
            }
            Adventurer publisherAdv = financeService.requireAdventurerByUserId(task.getPublisherId());
            long refund = Math.round(task.getRewardGold() * (1 - COMMISSION_RATE));
            if (refund > 0) {
                financeService.addGold(publisherAdv.getId(), refund, "REFUND", "TASK", taskId);
            }
            finish(task, record, ST_FAILED, 2, 0L, 0);
        }
    }

    private void finish(Task task, TaskAcceptRecord record, int taskStatus, int recordStatus, Long goldPaid, int purifyGained) {
        task.setStatus(taskStatus);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        record.setStatus(recordStatus);
        record.setGoldPaid(goldPaid);
        record.setPurifyGained(purifyGained);
        record.setFinishedAt(LocalDateTime.now());
        acceptRecordMapper.updateById(record);
    }

    /**
     * 冒险者协会管理员审核冒险者发布的任务
     */
    @Transactional
    public void auditAdventurerTask(Long adminUserId, Long taskId, boolean pass, String remark) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getSourceType() != SOURCE_ADVENTURER) {
            throw new BusinessException("任务不存在或非冒险者任务");
        }
        if (task.getStatus() != ST_PENDING_REVIEW) {
            throw new BusinessException("任务不在待审核状态");
        }
        task.setStatus(pass ? ST_OPEN : ST_REJECTED);
        task.setAuditId(adminUserId);
        task.setAuditRemark(remark);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        // 驳回则退还托管金币
        if (!pass) {
            Adventurer publisherAdv = financeService.requireAdventurerByUserId(task.getPublisherId());
            financeService.addGold(publisherAdv.getId(), task.getRewardGold(), "REFUND", "TASK", taskId);
        }
    }

    private void checkNotBanned(Adventurer adv) {
        if (adv.getBannedUntil() != null && adv.getBannedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(403, "冒险者协会已暂停对其开放至 "
                    + adv.getBannedUntil().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
    }

    public List<Task> listOpenTasks(Integer sourceType, String level, long page, long size) {
        return taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getStatus, ST_OPEN)
                .eq(sourceType != null, Task::getSourceType, sourceType)
                .eq(level != null && !level.isBlank(), Task::getTaskLevel, level)
                .orderByDesc(Task::getCreatedAt)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size));
    }

    public List<Task> listMyPublished(Long userId) {
        return taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getPublisherId, userId)
                .orderByDesc(Task::getCreatedAt)
                .last("LIMIT 50"));
    }

    public List<Task> listMyAccepted(Long userId) {
        List<Long> taskIds = acceptRecordMapper.selectList(new LambdaQueryWrapper<TaskAcceptRecord>()
                        .eq(TaskAcceptRecord::getAdventurerUserId, userId)
                        .orderByDesc(TaskAcceptRecord::getStartedAt)
                        .last("LIMIT 50"))
                .stream().map(TaskAcceptRecord::getTaskId).toList();
        return taskIds.isEmpty() ? List.of() : taskMapper.selectBatchIds(taskIds);
    }

    public List<Task> listPendingAudit() {
        return taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getSourceType, SOURCE_ADVENTURER)
                .eq(Task::getStatus, ST_PENDING_REVIEW)
                .orderByAsc(Task::getCreatedAt));
    }

    public List<XingzheApply> listPendingApplies() {
        return xingzheApplyMapper.selectList(new LambdaQueryWrapper<XingzheApply>()
                .eq(XingzheApply::getAuditStatus, 0)
                .orderByAsc(XingzheApply::getCreatedAt));
    }
}
