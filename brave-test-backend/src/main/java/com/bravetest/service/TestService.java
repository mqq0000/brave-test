package com.bravetest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bravetest.common.BusinessException;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.TestQuestion;
import com.bravetest.entity.TestResult;
import com.bravetest.mapper.AdventurerMapper;
import com.bravetest.mapper.TestQuestionMapper;
import com.bravetest.mapper.TestResultMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 觉醒性格测试引擎：勇气 / 理性 / 仁善三特质计分 -> 觉醒称号；负面选择累计初始污染
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final TestQuestionMapper questionMapper;
    private final TestResultMapper resultMapper;
    private final AdventurerMapper adventurerMapper;
    private final AdventurerService adventurerService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 觉醒称号：主特质 -> 称号 */
    private static final Map<String, String> TITLES = Map.of(
            "courage", "炽焰觉醒者",
            "rationality", "明澈贤者",
            "kindness", "微光行者");

    /**
     * 题目列表（剥离计分，只给题干与选项文本）
     */
    public List<Map<String, Object>> listQuestions() {
        List<TestQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<TestQuestion>()
                        .eq(TestQuestion::getStatus, 1)
                        .orderByAsc(TestQuestion::getSeq));
        List<Map<String, Object>> result = new ArrayList<>();
        for (TestQuestion q : questions) {
            try {
                List<Map<String, Object>> options = objectMapper.readValue(
                        q.getOptions(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                List<String> texts = options.stream().map(o -> String.valueOf(o.get("text"))).toList();
                Map<String, Object> item = new HashMap<>();
                item.put("id", q.getId());
                item.put("seq", q.getSeq());
                item.put("content", q.getContent());
                item.put("options", texts);
                result.add(item);
            } catch (Exception e) {
                log.error("题目 {} 选项JSON解析失败", q.getId(), e);
            }
        }
        return result;
    }

    /**
     * 提交作答：计算特质分与污染增量，落库并发称号
     *
     * @param answers [{questionId, optionIndex}]
     */
    @Transactional
    public Map<String, Object> submit(Long userId, List<Map<String, Object>> answers) {
        Adventurer adv = adventurerMapper.selectOne(
                new LambdaQueryWrapper<Adventurer>().eq(Adventurer::getUserId, userId));
        if (adv == null) {
            throw new BusinessException("冒险者档案不存在");
        }
        List<TestQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<TestQuestion>().eq(TestQuestion::getStatus, 1));
        if (answers == null || answers.size() < questions.size()) {
            throw new BusinessException("还有题目未作答");
        }

        int courage = 0, rationality = 0, kindness = 0, pollutionDelta = 0;
        StringBuilder answersJson = new StringBuilder("[");
        for (Map<String, Object> answer : answers) {
            Long qid = Long.valueOf(String.valueOf(answer.get("questionId")));
            int idx = Integer.parseInt(String.valueOf(answer.get("optionIndex")));
            TestQuestion q = questionMapper.selectById(qid);
            if (q == null) {
                continue;
            }
            try {
                List<Map<String, Object>> options = objectMapper.readValue(
                        q.getOptions(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                if (idx < 0 || idx >= options.size()) {
                    throw new BusinessException("题目 " + q.getSeq() + " 选项非法");
                }
                Map<String, Object> opt = options.get(idx);
                courage += intVal(opt.get("courage"));
                rationality += intVal(opt.get("rationality"));
                kindness += intVal(opt.get("kindness"));
                pollutionDelta += intVal(opt.get("pollution"));
                if (answersJson.length() > 1) {
                    answersJson.append(",");
                }
                answersJson.append("{\"q\":").append(qid).append(",\"o\":").append(idx).append("}");
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException("作答解析失败");
            }
        }
        answersJson.append("]");

        String title = resolveTitle(courage, rationality, kindness);
        TestResult result = new TestResult();
        result.setUserId(userId);
        result.setAdventurerId(adv.getId());
        result.setAnswers(answersJson.toString());
        result.setTraitCourage(courage);
        result.setTraitRationality(rationality);
        result.setTraitKindness(kindness);
        result.setPollutionDelta(pollutionDelta);
        result.setTitle(title);
        result.setCreatedAt(LocalDateTime.now());
        resultMapper.insert(result);

        // 负面选择带来的初始染污
        if (pollutionDelta > 0) {
            adventurerService.raisePollution(adv.getId(), pollutionDelta, "TEST", result.getId());
        }

        Map<String, Object> vo = new HashMap<>();
        vo.put("title", title);
        vo.put("courage", courage);
        vo.put("rationality", rationality);
        vo.put("kindness", kindness);
        vo.put("pollutionDelta", pollutionDelta);
        vo.put("resultId", result.getId());
        return vo;
    }

    public Map<String, Object> latestResult(Long userId) {
        TestResult result = resultMapper.selectOne(new LambdaQueryWrapper<TestResult>()
                .eq(TestResult::getUserId, userId)
                .orderByDesc(TestResult::getCreatedAt)
                .last("LIMIT 1"));
        if (result == null) {
            return null;
        }
        Map<String, Object> vo = new HashMap<>();
        vo.put("title", result.getTitle());
        vo.put("courage", result.getTraitCourage());
        vo.put("rationality", result.getTraitRationality());
        vo.put("kindness", result.getTraitKindness());
        vo.put("createdAt", result.getCreatedAt());
        return vo;
    }

    private String resolveTitle(int courage, int rationality, int kindness) {
        Map<String, Integer> traits = Map.of("courage", courage, "rationality", rationality, "kindness", kindness);
        return traits.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .map(Map.Entry::getKey)
                .findFirst()
                .map(TITLES::get)
                .orElse("萌芽守望者");
    }

    private int intVal(Object o) {
        return o == null ? 0 : Integer.parseInt(String.valueOf(o));
    }
}
