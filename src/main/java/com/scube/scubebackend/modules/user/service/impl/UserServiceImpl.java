package com.scube.scubebackend.modules.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.modules.admin.model.dto.AdminUserVO;
import com.scube.scubebackend.modules.user.mapper.UserMapper;
import com.scube.scubebackend.modules.user.model.dto.LoginRequest;
import com.scube.scubebackend.modules.user.model.dto.LoginResponse;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.modules.user.model.dto.UserVO;
import com.scube.scubebackend.modules.user.model.dto.UserProfileVO;
import com.scube.scubebackend.modules.user.model.entity.User;
import com.scube.scubebackend.modules.user.service.UserService;
import com.scube.scubebackend.util.JwtUtil;
import com.scube.scubebackend.util.UserContext;
import com.scube.scubebackend.util.DisplayIDGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private DisplayIDGenerator displayIDGenerator;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            String openId = request.getOpenId();
            if (openId == null || openId.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "openId不能为空");
            }

            // 查询或创建用户
            User user = userMapper.selectByOpenId(openId);
            
            if (user == null) {
                // 创建新用户
                user = new User();
                user.setOpenId(openId);
                user.setUserRole("USER");
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                user.setIsDelete(0);
                // 设置昵称和头像（如果提供）
                if (request.getNickname() != null && !request.getNickname().isEmpty()) {
                    System.out.println("Setting nickname: " + request.getNickname());
                    user.setNickname(request.getNickname());
                }
                if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
                    user.setAvatar(request.getAvatar());
                }
                // displayId
                String displayId;
                boolean isUnique;
                do {
                    displayId = displayIDGenerator.generateDisplayID();
                    isUnique = !userMapper.existsByDisplayId(displayId);
                } while (!isUnique);
                user.setDisplayId("U" + displayId);
                userMapper.insert(user);
            } else {
                // 更新现有用户的昵称和头像（如果提供且与现有不同）
                boolean needUpdate = false;
                if (request.getNickname() != null && !request.getNickname().isEmpty()) {
                    if (!request.getNickname().equals(user.getNickname())) {
                        user.setNickname(request.getNickname());
                        needUpdate = true;
                    }
                }
                if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
                    if (!request.getAvatar().equals(user.getAvatar())) {
                        user.setAvatar(request.getAvatar());
                        needUpdate = true;
                    }
                }
                if (needUpdate) {
                    user.setUpdateTime(LocalDateTime.now());
                    userMapper.updateById(user);
                }
            }
            
            // 生成JWT Token
            String token = jwtUtil.generateToken(user.getId(), user.getUserRole());
            
            // 构建响应
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            response.setUser(userVO);
            
            return response;
        } catch (BusinessException e) {
            // 业务异常直接抛出
            throw e;
        } catch (Exception e) {
            // 其他异常包装为业务异常，提供更友好的错误信息
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败：" + e.getMessage());
        }
    }
    
    @Override
    public UserVO getCurrentUser() {
        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public UserProfileVO getCurrentUserProfile() {
        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getDisplayId() != null ? user.getDisplayId() : String.valueOf(user.getId()));
        profile.setName(user.getNickname());
        profile.setEmail(null); // 目前数据库没有邮箱字段
        profile.setRole(user.getUserRole() != null ? user.getUserRole().toLowerCase() : "user");
        profile.setAvatar(user.getAvatar());
        profile.setJoinDate(user.getCreateTime() != null ? user.getCreateTime().toString() : null);

        return profile;
    }

    @Override
    public IPage<AdminUserVO> getAllUsers(int page, int pageSize) {
        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"ADMIN".equals(loginUser.getUserRole()) && !"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问");
        }

        Page<User> userPage = new Page<>(page, pageSize);
        IPage<User> resultPage = userMapper.selectPage(userPage, null);

        List<AdminUserVO> voList = resultPage.getRecords().stream().map(user -> {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(String.valueOf(user.getId()));
            vo.setName(user.getNickname());
            vo.setEmail(""); // Email not in DB
            vo.setRole(user.getUserRole());
            vo.setJoinDate(user.getCreateTime() != null ? user.getCreateTime().toString().split("T")[0] : "");
            return vo;
        }).collect(Collectors.toList());

        Page<AdminUserVO> voPage = new Page<>(page, pageSize, resultPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}
