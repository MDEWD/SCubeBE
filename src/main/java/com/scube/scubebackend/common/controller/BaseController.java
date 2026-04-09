package com.scube.scubebackend.common.controller;

import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.util.UserContext;

public class BaseController {
    
    protected LoginUser getLoginUser() {
        return UserContext.getUser();
    }
}

