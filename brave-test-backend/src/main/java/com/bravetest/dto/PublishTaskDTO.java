package com.bravetest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "冒险者协会发布任务请求")
public class PublishTaskDTO {

    @NotBlank(message = "任务标题不能为空")
    @Schema(description = "标题")
    private String title;

    @Schema(description = "任务详情")
    private String description;

    @NotBlank(message = "任务等级不能为空")
    @Schema(description = "任务等级 D/C/B/A/S")
    private String taskLevel;

    @NotNull(message = "报酬金币不能为空")
    @Schema(description = "报酬金币（需符合等级区间，协会抽成10%后接取者得90%）")
    private Long rewardGold;
}
