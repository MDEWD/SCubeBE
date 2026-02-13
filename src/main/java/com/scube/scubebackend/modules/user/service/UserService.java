package com.scube.scubebackend.modules.user.service;

import com.scube.scubebackend.modules.user.model.dto.LoginRequest;
import com.scube.scubebackend.modules.user.model.dto.LoginResponse;
import com.scube.scubebackend.modules.user.model.dto.UserVO;

public interface UserService {
    LoginResponse login(LoginRequest request);
    UserVO getCurrentUser();
}

