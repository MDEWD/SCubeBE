package com.scube.scubebackend.modules.admin.model.dto;

import lombok.Data;

@Data
public class AuditDecision {
    private String status;
    private String rejectReason;
}
