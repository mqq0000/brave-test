package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.PurificationRecord;
import com.bravetest.mapper.AdventurerMapper;
import com.bravetest.mapper.PurificationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 冒险者档案服务：萌芽污染 / 净化 / 状态推导
 */
@Service
@RequiredArgsConstructor
public class AdventurerService {

    /** 完成任务数排行榜 Redis ZSet key */
    public static final String RANK_KEY = "rank:completed";

    private final AdventurerMapper adventurerMapper;
    private final PurificationRecordMapper purificationRecordMapper;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    private final com.bravetest.mapper.UserMapper userMapper;

    public Adventurer getByUserId(Long userId) {
        Adventurer adv = adventurerMapper.selectOne(
                new LambdaQueryWrapper<Adventurer>().eq(Adventurer::getUserId, userId));
        if (adv == null) {
            throw new BusinessException("冒险者档案不存在");
        }
        return adv;
    }

    /**
     * 污染值 -> 萌芽状态：0~39正常 40~69灰化 70~100赤化
     */
    public static String deriveSproutStatus(int pollution) {
        if (pollution >= 70) {
            return "RED";
        }
        if (pollution >= 40) {
            return "GRAY";
        }
        return "NORMAL";
    }

    /**
     * 染污（剧情负面选择/时间流逝），污染值随之上限100并推导萌芽状态
     */
    @Transactional
    public Adventurer raisePollution(Long adventurerId, int value, String source, Long refId) {
        Adventurer adv = adventurerMapper.selectById(adventurerId);
        if (adv == null) {
            throw new BusinessException("冒险者不存在");
        }
        int before = adv.getPollutionValue() == null ? 0 : adv.getPollutionValue();
        int after = Math.min(100, before + value);
        adventurerMapper.update(null, new LambdaUpdateWrapper<Adventurer>()
                .eq(Adventurer::getId, adventurerId)
                .set(Adventurer::getPollutionValue, after)
                .set(Adventurer::getSproutStatus, deriveSproutStatus(after))
                .set(Adventurer::getUpdatedAt, LocalDateTime.now()));
        PurificationRecord record = new PurificationRecord();
        record.setAdventurerId(adventurerId);
        record.setSource(source);
        record.setValueBefore(before);
        record.setValueAfter(after);
        record.setRefId(refId);
        record.setCreatedAt(LocalDateTime.now());
        purificationRecordMapper.insert(record);
        adv.setPollutionValue(after);
        adv.setSproutStatus(deriveSproutStatus(after));
        return adv;
    }

    /**
     * 净化（善事任务/净化剂/知识收录）
     */
    @Transactional
    public Adventurer purify(Long adventurerId, int value, String source, Long refId) {
        Adventurer adv = adventurerMapper.selectById(adventurerId);
        if (adv == null) {
            throw new BusinessException("冒险者不存在");
        }
        int before = adv.getPollutionValue() == null ? 0 : adv.getPollutionValue();
        int after = Math.max(0, before - value);
        adventurerMapper.update(null, new LambdaUpdateWrapper<Adventurer>()
                .eq(Adventurer::getId, adventurerId)
                .set(Adventurer::getPollutionValue, after)
                .set(Adventurer::getSproutStatus, deriveSproutStatus(after))
                .set(Adventurer::getUpdatedAt, LocalDateTime.now()));

        PurificationRecord record = new PurificationRecord();
        record.setAdventurerId(adventurerId);
        record.setSource(source);
        record.setValueBefore(before);
        record.setValueAfter(after);
        record.setRefId(refId);
        record.setCreatedAt(LocalDateTime.now());
        purificationRecordMapper.insert(record);
        adv.setPollutionValue(after);
        adv.setSproutStatus(deriveSproutStatus(after));
        return adv;
    }

    /**
     * 写入完成任务数排行榜
     */
    public void recordRank(Long userId, long completedCount) {
        redisTemplate.opsForZSet().add(RANK_KEY, String.valueOf(userId), completedCount);
    }

    /**
     * 完成任务数排行榜 Top N（Redis ZSet）
     */
    public java.util.List<java.util.Map<String, Object>> topRank(int n) {
        var tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(RANK_KEY, 0, Math.max(0, n - 1));
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        if (tuples == null) {
            return result;
        }
        int rank = 1;
        for (var tuple : tuples) {
            Long userId = Long.valueOf(java.util.Objects.requireNonNull(tuple.getValue()));
            var user = userMapper.selectById(userId);
            Adventurer adv = adventurerMapper.selectOne(
                    new LambdaQueryWrapper<Adventurer>().eq(Adventurer::getUserId, userId));
            if (user == null || adv == null) {
                continue;
            }
            result.add(java.util.Map.of(
                    "rank", rank++,
                    "nickname", user.getNickname(),
                    "rankLevel", adv.getRankLevel(),
                    "completedCount", tuple.getScore() == null ? 0 : tuple.getScore().longValue()));
        }
        return result;
    }

    /**
     * 金币流水VO占位（P3 完善分页查询）
     */
    public record GoldFlowVO(Long id, String bizType, Long amount, LocalDateTime createdAt) {
    }
}
