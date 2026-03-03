package com.scube.scubebackend.modules.product.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class MyProductQueryRequest {
    private Integer page;
    private Integer pageSize;
    private List<String> gpuTypes;
    private String publishTimeStart;
    private String publishTimeEnd;
}

