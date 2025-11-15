package com.scube.scubebackend.util;

import com.scube.scubebackend.model.dto.LoginUser;

public class UserContext {
    private static final ThreadLocal<LoginUser> userHolder = new ThreadLocal<>();
    
    public static void setUser(LoginUser user) {
        userHolder.set(user);
    }
    
    public static LoginUser getUser() {
        return userHolder.get();
    }
    
    public static void clear() {
        userHolder.remove();
    }
}

