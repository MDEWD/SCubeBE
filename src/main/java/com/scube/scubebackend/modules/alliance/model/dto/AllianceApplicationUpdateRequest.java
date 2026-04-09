package com.scube.scubebackend.modules.alliance.model.dto;

import lombok.Data;

@Data
public class AllianceApplicationUpdateRequest {
    private String kind;

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
}

