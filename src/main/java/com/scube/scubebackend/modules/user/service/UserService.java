package com.scube.scubebackend.modules.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scube.scubebackend.modules.admin.model.dto.AdminUserVO;
import com.scube.scubebackend.modules.user.model.dto.LoginRequest;
import com.scube.scubebackend.modules.user.model.dto.LoginResponse;
import com.scube.scubebackend.modules.user.model.dto.UserProfileVO;
import com.scube.scubebackend.modules.user.model.dto.UserVO;

public interface UserService {
    LoginResponse login(LoginRequest request);
    UserVO getCurrentUser();
    UserProfileVO getCurrentUserProfile();
    IPage<AdminUserVO> getAllUsers(int page, int pageSize);
}
