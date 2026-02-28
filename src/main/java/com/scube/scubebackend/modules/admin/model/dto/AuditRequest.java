package com.scube.scubebackend.modules.admin.model.dto;

import lombok.Data;

@Data
public class AuditRequest {
    private String id;
    private String userName;
    private String applyTime;
    private String status;
    private String reason;
}
