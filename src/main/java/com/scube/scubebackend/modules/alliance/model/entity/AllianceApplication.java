package com.scube.scubebackend.modules.alliance.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alliance_application")
public class AllianceApplication {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField("user_display_id")
    private String userDisplayId;

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

    private String status;

    private String rejectReason;

    private LocalDateTime reviewTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;
}

