package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.PlayerStoryProgress;
import com.bravetest.entity.StoryChapter;
import com.bravetest.entity.StoryNode;
import com.bravetest.entity.StoryOption;
import com.bravetest.mapper.PlayerStoryProgressMapper;
import com.bravetest.mapper.StoryChapterMapper;
import com.bravetest.mapper.StoryNodeMapper;
import com.bravetest.mapper.StoryOptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文字冒险剧情引擎：章节解锁链 -> 节点渲染 -> 选择结算（污染/金币） -> 下一节点
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryChapterMapper chapterMapper;
    private final StoryNodeMapper nodeMapper;
    private final StoryOptionMapper optionMapper;
    private final PlayerStoryProgressMapper progressMapper;
    private final FinanceService financeService;
    private final AdventurerService adventurerService;

    /**
     * 章节列表（含解锁/完成状态）
     */
    public List<Map<String, Object>> listChapters(Long userId) {
        List<StoryChapter> chapters = chapterMapper.selectList(
                new LambdaQueryWrapper<StoryChapter>().orderByAsc(StoryChapter::getSeq));
        List<Map<String, Object>> result = new ArrayList<>();
        boolean prevCompleted = true;
        for (StoryChapter chapter : chapters) {
            PlayerStoryProgress progress = progressMapper.selectOne(
                    new LambdaQueryWrapper<PlayerStoryProgress>()
                            .eq(PlayerStoryProgress::getUserId, userId)
                            .eq(PlayerStoryProgress::getChapterId, chapter.getId())
                            .orderByDesc(PlayerStoryProgress::getUpdatedAt)
                            .last("LIMIT 1"));
            boolean unlocked = prevCompleted;
            boolean completed = progress != null && progress.getStatus() == 1;
            Map<String, Object> item = new HashMap<>();
            item.put("id", chapter.getId());
            item.put("seq", chapter.getSeq());
            item.put("title", chapter.getTitle());
            item.put("unlocked", unlocked);
            item.put("completed", completed);
            item.put("inProgress", progress != null && progress.getStatus() == 0);
            result.add(item);
            prevCompleted = completed;
        }
        return result;
    }

    /**
     * 进入章节：无进度则从首节点开始；返回当前节点渲染
     */
    @Transactional
    public Map<String, Object> enterChapter(Long userId, Long chapterId) {
        AdventurerProfileCheck(userId);
        StoryChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        PlayerStoryProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<PlayerStoryProgress>()
                        .eq(PlayerStoryProgress::getUserId, userId)
                        .eq(PlayerStoryProgress::getChapterId, chapterId)
                        .eq(PlayerStoryProgress::getStatus, 0)
                        .last("LIMIT 1"));
        if (progress == null) {
            StoryNode first = nodeMapper.selectOne(new LambdaQueryWrapper<StoryNode>()
                    .eq(StoryNode::getChapterId, chapterId)
                    .orderByAsc(StoryNode::getSeq)
                    .last("LIMIT 1"));
            if (first == null) {
                throw new BusinessException("章节内容缺失");
            }
            progress = new PlayerStoryProgress();
            progress.setAdventurerId(financeService.requireAdventurerByUserId(userId).getId());
            progress.setUserId(userId);
            progress.setChapterId(chapterId);
            progress.setCurrentNodeId(first.getId());
            progress.setStatus(0);
            progress.setChoicesLog("[]");
            progress.setCreatedAt(LocalDateTime.now());
            progress.setUpdatedAt(LocalDateTime.now());
            progressMapper.insert(progress);
        }
        return renderNode(progress);
    }

    /**
     * 做出选择：结算污染/金币，推进节点；next 为空则本章完结
     */
    @Transactional
    public Map<String, Object> choose(Long userId, Long optionId) {
        StoryOption option = optionMapper.selectById(optionId);
        if (option == null) {
            throw new BusinessException("选项不存在");
        }
        PlayerStoryProgress progress = progressMapper.selectOne(
                new LambdaQueryWrapper<PlayerStoryProgress>()
                        .eq(PlayerStoryProgress::getUserId, userId)
                        .eq(PlayerStoryProgress::getCurrentNodeId, option.getNodeId())
                        .eq(PlayerStoryProgress::getStatus, 0)
                        .last("LIMIT 1"));
        if (progress == null) {
            throw new BusinessException("剧情进度不存在或已完成");
        }
        if (!progress.getCurrentNodeId().equals(option.getNodeId())) {
            throw new BusinessException("当前进度不在此节点，无法选择");
        }

        Adventurer adv = financeService.requireAdventurerByUserId(userId);
        // 金币结算
        if (option.getGoldDelta() != null && option.getGoldDelta() > 0) {
            financeService.addGold(adv.getId(), option.getGoldDelta(), "STORY_REWARD", "STORY", progress.getChapterId());
        }
        // 污染结算：正=染污 负=净化
        int pd = option.getPollutionDelta() == null ? 0 : option.getPollutionDelta();
        if (pd > 0) {
            adventurerService.raisePollution(adv.getId(), pd, "STORY", progress.getChapterId());
        } else if (pd < 0) {
            adventurerService.purify(adv.getId(), -pd, "STORY", progress.getChapterId());
        }

        // 选择记录
        String log = progress.getChoicesLog() == null ? "[]" : progress.getChoicesLog();
        String entry = "{\"optionId\":" + optionId + ",\"text\":\"" + option.getText().replace("\"", "'") + "\"}";
        String newLog = log.equals("[]") ? "[" + entry + "]" : log.substring(0, log.length() - 1) + "," + entry + "]";

        boolean chapterEnd = option.getNextNodeId() == null;
        progress.setCurrentNodeId(chapterEnd ? progress.getCurrentNodeId() : option.getNextNodeId());
        progress.setStatus(chapterEnd ? 1 : 0);
        progress.setChoicesLog(newLog);
        progress.setUpdatedAt(LocalDateTime.now());
        progressMapper.updateById(progress);

        if (chapterEnd) {
            Map<String, Object> result = new HashMap<>();
            result.put("finished", true);
            result.put("message", "本章完结。你的每一个选择，都在塑造你的萌芽。");
            return result;
        }
        return renderNode(progress);
    }

    private Map<String, Object> renderNode(PlayerStoryProgress progress) {
        StoryNode node = nodeMapper.selectById(progress.getCurrentNodeId());
        if (node == null) {
            throw new BusinessException("剧情节点缺失");
        }
        List<StoryOption> options = optionMapper.selectList(
                new LambdaQueryWrapper<StoryOption>()
                        .eq(StoryOption::getNodeId, node.getId())
                        .orderByAsc(StoryOption::getSeq));
        Map<String, Object> result = new HashMap<>();
        result.put("progressId", progress.getId());
        result.put("nodeType", node.getNodeType());
        result.put("content", node.getContent());
        result.put("options", options.stream().map(o -> Map.of(
                "id", o.getId(),
                "text", o.getText())).toList());
        return result;
    }

    private void AdventurerProfileCheck(Long userId) {
        financeService.requireAdventurerByUserId(userId);
    }
}
