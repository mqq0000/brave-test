package com.bravetest;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.Task;
import com.bravetest.entity.TaskAcceptRecord;
import com.bravetest.common.BusinessException;
import com.bravetest.mapper.AdventurerMapper;
import com.bravetest.mapper.TaskAcceptRecordMapper;
import com.bravetest.mapper.TaskMapper;
import com.bravetest.mapper.XingzheApplyMapper;
import com.bravetest.service.AdventurerService;
import com.bravetest.service.FinanceService;
import com.bravetest.service.TaskService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务升降级核心规则单元测试：
 * 越级成功直升 + 1万奖励 / 越级失败封禁30天 + 退发布者90% / 每100件自然升级
 */
class TaskServiceTest {

    private TaskMapper taskMapper;
    private TaskAcceptRecordMapper acceptRecordMapper;
    private AdventurerMapper adventurerMapper;
    private FinanceService financeService;
    private AdventurerService adventurerService;
    private TaskService service;

    @BeforeAll
    static void initTableInfo() {
        // 单测环境无 MP 运行时，需手动初始化 Lambda 缓存
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Task.class);
        TableInfoHelper.initTableInfo(assistant, Adventurer.class);
        TableInfoHelper.initTableInfo(assistant, TaskAcceptRecord.class);
    }

    @BeforeEach
    void setUp() {
        taskMapper = mock(TaskMapper.class);
        acceptRecordMapper = mock(TaskAcceptRecordMapper.class);
        adventurerMapper = mock(AdventurerMapper.class);
        financeService = mock(FinanceService.class);
        adventurerService = mock(AdventurerService.class);
        service = new TaskService(taskMapper, acceptRecordMapper, adventurerMapper,
                mock(XingzheApplyMapper.class), financeService, adventurerService,
                mock(StringRedisTemplate.class));
    }

    private Task task() {
        Task t = new Task();
        t.setId(1L);
        t.setSourceType(2);
        t.setPublisherId(10L);
        t.setTaskLevel("A");
        t.setRewardGold(100000L);
        t.setAcceptorGold(90000L);
        t.setPurifyValue(10);
        t.setStatus(TaskService.ST_TO_VERIFY);
        t.setAcceptorId(20L);
        return t;
    }

    private TaskAcceptRecord record(int crossLevel) {
        TaskAcceptRecord r = new TaskAcceptRecord();
        r.setId(100L);
        r.setTaskId(1L);
        r.setAdventurerId(5L);
        r.setAdventurerUserId(20L);
        r.setIsCrossLevel(crossLevel);
        r.setStatus(0);
        return r;
    }

    private Adventurer acceptor() {
        Adventurer a = new Adventurer();
        a.setId(5L);
        a.setUserId(20L);
        a.setRankLevel("D");
        a.setCompletedCount(0);
        a.setPollutionValue(30);
        return a;
    }

    @Test
    void levelIndex_validAndInvalid() {
        assertEquals(0, TaskService.levelIndex("D"));
        assertEquals(2, TaskService.levelIndex("b"));
        assertEquals(4, TaskService.levelIndex("S"));
        assertThrows(BusinessException.class, () -> TaskService.levelIndex("X"));
    }

    @Test
    void verifyTask_crossLevelSuccess_promotesDirectlyWithBonus() {
        Task t = task();
        TaskAcceptRecord r = record(1);
        Adventurer acc = acceptor();
        when(taskMapper.selectById(1L)).thenReturn(t);
        when(acceptRecordMapper.selectOne(any())).thenReturn(r);
        when(adventurerMapper.selectById(5L)).thenReturn(acc);

        // 发布者验收
        service.verifyTask(10L, 1L, true, false);

        // 越级成功：发放90%报酬
        verify(financeService).addGold(eq(5L), eq(90000L), eq("TASK_REWARD"), eq("TASK"), eq(1L));
        // 越级直升 A 级并发放 1 万升级奖励
        verify(financeService).addGold(eq(5L), eq(10000L), eq("LEVEL_UP_REWARD"), eq("ADVENTURER"), eq(5L));
        // 任务与接单记录状态流转
        assertEquals(TaskService.ST_COMPLETED, t.getStatus());
        assertEquals(1, r.getStatus());
        assertEquals(90000L, r.getGoldPaid());
        // 完成计数 +1 并写排行榜
        verify(adventurerService).recordRank(eq(20L), eq(1L));
    }

    @Test
    void verifyTask_crossLevelFail_bansAndRefundsPublisher() {
        Task t = task();
        TaskAcceptRecord r = record(1);
        Adventurer acc = acceptor();
        Adventurer publisher = new Adventurer();
        publisher.setId(30L);
        when(taskMapper.selectById(1L)).thenReturn(t);
        when(acceptRecordMapper.selectOne(any())).thenReturn(r);
        when(adventurerMapper.selectById(5L)).thenReturn(acc);
        when(financeService.requireAdventurerByUserId(10L)).thenReturn(publisher);

        service.verifyTask(10L, 1L, false, false);

        // 越级失败：不发放报酬，退还发布者托管报酬90%
        verify(financeService, never()).addGold(eq(5L), anyLong(), any(), any(), anyLong());
        verify(financeService).addGold(eq(30L), eq(90000L), eq("REFUND"), eq("TASK"), eq(1L));
        assertEquals(TaskService.ST_FAILED, t.getStatus());
        assertEquals(2, r.getStatus());
        // 封禁动作已对冒险者档案执行
        verify(adventurerMapper).update(any(), any());
    }

    @Test
    void verifyTask_notPublisherOrAdmin_forbidden() {
        Task t = task();
        when(taskMapper.selectById(1L)).thenReturn(t);
        when(acceptRecordMapper.selectOne(any())).thenReturn(record(0));
        when(adventurerMapper.selectById(5L)).thenReturn(acceptor());

        assertThrows(BusinessException.class,
                () -> service.verifyTask(999L, 1L, true, false));
    }

    @Test
    void verifyTask_naturalPromotion_at100Completions() {
        Task t = task();
        // 非越级任务
        t.setTaskLevel("D");
        t.setAcceptorGold(90L);
        t.setRewardGold(100L);
        TaskAcceptRecord r = record(0);
        Adventurer acc = acceptor();
        acc.setCompletedCount(99); // 完成99件后再完成1件 -> 自然升级
        when(taskMapper.selectById(1L)).thenReturn(t);
        when(acceptRecordMapper.selectOne(any())).thenReturn(r);
        when(adventurerMapper.selectById(5L)).thenReturn(acc);

        service.verifyTask(10L, 1L, true, false);

        // 自然升级奖励发放
        verify(financeService).addGold(eq(5L), eq(10000L), eq("LEVEL_UP_REWARD"), eq("ADVENTURER"), eq(5L));
        // 90% 报酬也发放
        verify(financeService).addGold(eq(5L), eq(90L), eq("TASK_REWARD"), eq("TASK"), eq(1L));
    }
}
