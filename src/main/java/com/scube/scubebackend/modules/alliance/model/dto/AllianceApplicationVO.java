package com.scube.scubebackend.modules.alliance.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AllianceApplicationVO {
    private Long id;
    private String kind;
    private String status;
    private String rejectReason;

    private String realName;
    private String phone;
    private String idNumber;
    private String idFrontImage;
    private String idBackImage;

    private String orgName;
    private String creditCode;
    private String licenseImage;
    private String job;
    private String mainBusiness;
    private String contactName;
    private String contactMethod;

    private String userDisplayId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime reviewTime;
}

