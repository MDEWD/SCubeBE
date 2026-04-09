package com.scube.scubebackend.modules.portal.model.dto;

import lombok.Data;

@Data
public class CarouselVO {
    private Long id;
    private String title;
    private String summary;
    private String image;
    private String link;
    private Integer order;
}

