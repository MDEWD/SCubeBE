package com.scube.scubebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        String code = request.getCode();
        String openId = request.getOpenId();
        
        // 从Redis获取验证码
        String key = "login:code:" + openId;
        String storedCode = (String) redisTemplate.opsForValue().get(key);
        
        if (storedCode == null || !storedCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或已过期");
        }
        
        // 验证成功后删除验证码（一次性使用）
        redisTemplate.delete(key);
        
        // 查询或创建用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getOpenId, openId);
        User user = userMapper.selectOne(queryWrapper);
        
        if (user == null) {
            // 创建新用户
            user = new User();
            user.setOpenId(openId);
            user.setUserRole("USER");
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            user.setIsDelete(0);
            userMapper.insert(user);
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

