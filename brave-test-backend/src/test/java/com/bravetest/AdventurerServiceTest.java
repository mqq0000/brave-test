package com.bravetest;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.bravetest.entity.Adventurer;
import com.bravetest.entity.PurificationRecord;
import com.bravetest.mapper.AdventurerMapper;
import com.bravetest.mapper.PurificationRecordMapper;
import com.bravetest.mapper.UserMapper;
import com.bravetest.service.AdventurerService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 萌芽污染状态机单元测试：三档状态推导 + 污染/净化的边界钳制
 */
class AdventurerServiceTest {

    @org.junit.jupiter.api.BeforeAll
    static void initTableInfo() {
        // 单测环境无 MP 运行时，需手动初始化 Lambda 缓存
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Adventurer.class);
    }

    private AdventurerMapper adventurerMapper;
    private PurificationRecordMapper purificationRecordMapper;
    private AdventurerService service;

    @BeforeEach
    void setUp() {
        adventurerMapper = mock(AdventurerMapper.class);
        purificationRecordMapper = mock(PurificationRecordMapper.class);
        service = new AdventurerService(adventurerMapper, purificationRecordMapper,
                mock(StringRedisTemplate.class), mock(UserMapper.class));
    }

    @Test
    void deriveSproutStatus_boundaries() {
        assertEquals("NORMAL", AdventurerService.deriveSproutStatus(0));
        assertEquals("NORMAL", AdventurerService.deriveSproutStatus(39));
        assertEquals("GRAY", AdventurerService.deriveSproutStatus(40));
        assertEquals("GRAY", AdventurerService.deriveSproutStatus(69));
        assertEquals("RED", AdventurerService.deriveSproutStatus(70));
        assertEquals("RED", AdventurerService.deriveSproutStatus(100));
    }

    @Test
    void raisePollution_clampsAt100_andWritesRecord() {
        Adventurer adv = new Adventurer();
        adv.setId(5L);
        adv.setPollutionValue(90);
        when(adventurerMapper.selectById(5L)).thenReturn(adv);

        Adventurer result = service.raisePollution(5L, 50, "STORY", 9L);

        // 90 + 50 被钳制为 100，且状态推导为赤化
        assertEquals(100, result.getPollutionValue());
        assertEquals("RED", result.getSproutStatus());
        ArgumentCaptor<PurificationRecord> captor = ArgumentCaptor.forClass(PurificationRecord.class);
        verify(purificationRecordMapper).insert(captor.capture());
        assertEquals(90, captor.getValue().getValueBefore());
        assertEquals(100, captor.getValue().getValueAfter());
        assertEquals("STORY", captor.getValue().getSource());
        assertEquals(9L, captor.getValue().getRefId());
    }

    @Test
    void purify_clampsAt0_andRestoresNormal() {
        Adventurer adv = new Adventurer();
        adv.setId(5L);
        adv.setPollutionValue(45);
        when(adventurerMapper.selectById(5L)).thenReturn(adv);

        Adventurer result = service.purify(5L, 100, "PURIFIER", 1L);

        // 45 - 100 被钳制为 0，回到健康状态
        assertEquals(0, result.getPollutionValue());
        assertEquals("NORMAL", result.getSproutStatus());
    }

    @Test
    void purify_missingAdventurer_throws() {
        when(adventurerMapper.selectById(999L)).thenReturn(null);
        assertThrows(com.bravetest.common.BusinessException.class,
                () -> service.purify(999L, 10, "XINGZHE_TASK", 1L));
        verify(adventurerMapper, org.mockito.Mockito.never())
                .update(isNull(), any());
    }

    @Test
    void raisePollution_zeroValue_keepsStatusDerived() {
        Adventurer adv = new Adventurer();
        adv.setId(5L);
        adv.setPollutionValue(50);
        when(adventurerMapper.selectById(5L)).thenReturn(adv);

        Adventurer result = service.raisePollution(5L, 0, "TEST", null);
        assertEquals(50, result.getPollutionValue());
        assertEquals("GRAY", result.getSproutStatus());
        verify(adventurerMapper).update(isNull(), any());
        verify(purificationRecordMapper).insert(any(PurificationRecord.class));
    }
}
