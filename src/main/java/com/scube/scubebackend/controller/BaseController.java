package com.scube.scubebackend.controller;

import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.util.UserContext;

public class BaseController {
    
    protected LoginUser getLoginUser() {
        return UserContext.getUser();
    }
}

