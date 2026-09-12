package com.bravetest.controller;

import com.bravetest.common.R;
import com.bravetest.common.UserContext;
import com.bravetest.dto.PublishTaskDTO;
import com.bravetest.dto.XingzheApplyDTO;
import com.bravetest.entity.Task;
import com.bravetest.entity.XingzheApply;
import com.bravetest.mapper.XingzheApplyMapper;
import com.bravetest.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "03-任务中心")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final XingzheApplyMapper xingzheApplyMapper;

    @Operation(summary = "任务大厅（sourceType: 1行者 2冒险者；level: D/C/B/A/S）")
    @GetMapping("/tasks")
    public R<List<Task>> list(@RequestParam(required = false) Integer sourceType,
                              @RequestParam(required = false) String level,
                              @RequestParam(defaultValue = "1") long page,
                              @RequestParam(defaultValue = "10") long size) {
        return R.ok(taskService.listOpenTasks(sourceType, level, page, size));
    }

    @Operation(summary = "发布冒险者任务（预付托管，协会抽成10%，待管理员审核）")
    @PostMapping("/adventurer/tasks")
    public R<Long> publish(@Valid @RequestBody PublishTaskDTO dto) {
        return R.ok(taskService.publishAdventurerTask(UserContext.getUserId(),
                dto.getTitle(), dto.getDescription(), dto.getTaskLevel(), dto.getRewardGold()));
    }

    @Operation(summary = "接单（越级需 confirmCrossLevel=true 二次确认）")
    @PostMapping("/tasks/{id}/accept")
    public R<Long> accept(@PathVariable Long id,
                          @RequestParam(defaultValue = "false") boolean confirmCrossLevel) {
        return R.ok(taskService.acceptTask(UserContext.getUserId(), id, confirmCrossLevel));
    }

    @Operation(summary = "提交成果，任务进入待验收")
    @PostMapping("/tasks/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        taskService.submitTask(UserContext.getUserId(), id);
        return R.ok();
    }

    @Operation(summary = "验收（发布者操作：success=true 结算升级 / false 越级失败封禁30天）")
    @PostMapping("/tasks/{id}/verify")
    public R<Void> verify(@PathVariable Long id, @RequestParam boolean success) {
        taskService.verifyTask(UserContext.getUserId(), id, success, false);
        return R.ok();
    }

    @Operation(summary = "向行者协会提交任务帮助申请")
    @PostMapping("/xingzhe/apply")
    public R<Long> xingzheApply(@Valid @RequestBody XingzheApplyDTO dto) {
        return R.ok(taskService.submitXingzheApply(UserContext.getUserId(), dto.getTitle(), dto.getContent()));
    }

    @Operation(summary = "我发布的任务")
    @GetMapping("/tasks/mine/published")
    public R<List<Task>> myPublished() {
        return R.ok(taskService.listMyPublished(UserContext.getUserId()));
    }

    @Operation(summary = "我接取的任务")
    @GetMapping("/tasks/mine/accepted")
    public R<List<Task>> myAccepted() {
        return R.ok(taskService.listMyAccepted(UserContext.getUserId()));
    }

    @Operation(summary = "我的行者申请记录")
    @GetMapping("/xingzhe/applies/mine")
    public R<List<XingzheApply>> myApplies() {
        return R.ok(xingzheApplyMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<XingzheApply>()
                        .eq(XingzheApply::getApplicantId, UserContext.getUserId())
                        .orderByDesc(XingzheApply::getCreatedAt)));
    }
}
