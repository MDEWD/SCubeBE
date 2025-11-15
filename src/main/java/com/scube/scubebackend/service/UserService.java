package com.scube.scubebackend.service;

import com.scube.scubebackend.model.dto.LoginRequest;
import com.scube.scubebackend.model.dto.LoginResponse;
import com.scube.scubebackend.model.dto.UserVO;

public interface UserService {
    LoginResponse login(LoginRequest request);
    UserVO getCurrentUser();
}

