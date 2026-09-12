package com.bravetest.controller;

import com.bravetest.common.R;
import com.bravetest.common.UserContext;
import com.bravetest.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "06-冒险剧情")
@RestController
@RequestMapping("/api/v1/story")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @Operation(summary = "章节列表（含解锁/完成状态）")
    @GetMapping("/chapters")
    public R<List<Map<String, Object>>> chapters() {
        return R.ok(storyService.listChapters(UserContext.getUserId()));
    }

    @Operation(summary = "进入章节，返回当前节点与选项")
    @PostMapping("/chapters/{id}/enter")
    public R<Map<String, Object>> enter(@PathVariable Long id) {
        return R.ok(storyService.enterChapter(UserContext.getUserId(), id));
    }

    @Operation(summary = "做出选择（结算污染/金币并推进剧情）")
    @PostMapping("/choose")
    public R<Map<String, Object>> choose(@RequestParam Long optionId) {
        return R.ok(storyService.choose(UserContext.getUserId(), optionId));
    }
}
