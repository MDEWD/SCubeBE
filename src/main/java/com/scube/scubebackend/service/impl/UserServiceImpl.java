package com.scube.scubebackend.service.impl;

import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.mapper.UserMapper;
import com.scube.scubebackend.model.dto.LoginRequest;
import com.scube.scubebackend.model.dto.LoginResponse;
import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.UserVO;
import com.scube.scubebackend.model.entity.User;
import com.scube.scubebackend.service.UserService;
import com.scube.scubebackend.util.JwtUtil;
import com.scube.scubebackend.util.UserContext;
import com.scube.scubebackend.util.DisplayIDGenerator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
            String code = request.getCode();
            String openId = request.getOpenId();
            
            if (code == null || code.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
            }
            if (openId == null || openId.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "openId不能为空");
            }
            
            // 从Redis获取验证码
            String key = "login:code:" + openId;
            String storedCode = (String) redisTemplate.opsForValue().get(key);
            
            if (storedCode == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期，请重新获取");
            }
            
            if (!storedCode.equals(code)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
            }
            
            // 验证成功后删除验证码（一次性使用）
            redisTemplate.delete(key);
            
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
                user.setDisplayId(displayId);
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
}

