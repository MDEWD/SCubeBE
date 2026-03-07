package com.scube.scubebackend.modules.alliance.model.dto;

import lombok.Data;

@Data
public class AllianceApplicationReviewRequest {

    /**
     * 审核状态（建议值：APPROVED / REJECTED；具体以你的业务枚举为准）
     */
    private String status;

    /**
     * 驳回原因（status=REJECTED 时使用）
     */
    private String rejectReason;
}
