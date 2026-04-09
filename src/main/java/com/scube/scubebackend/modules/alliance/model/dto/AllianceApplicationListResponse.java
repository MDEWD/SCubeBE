package com.scube.scubebackend.modules.alliance.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class AllianceApplicationListResponse {
    private long total;
    private List<AllianceApplicationVO> items;
}

