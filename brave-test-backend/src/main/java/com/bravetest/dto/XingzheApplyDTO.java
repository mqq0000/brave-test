package com.bravetest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "行者协会任务帮助申请")
public class XingzheApplyDTO {

    @NotBlank(message = "标题不能为空")
    @Schema(description = "申请标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "申请内容（希望行者协会提供何种帮助任务）")
    private String content;
}
