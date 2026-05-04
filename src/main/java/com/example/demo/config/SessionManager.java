package com.example.demo.config;

import com.example.demo.entity.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的会话管理器（基于内存）
 */
@Component
public class SessionManager {
    
    private static final Map<String, User> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000; // 30分钟超时
    
    /**
     * 创建会话
     */
    public String createSession(User user) {
        String sessionId = generateSessionId();
        sessions.put(sessionId, user);
        return sessionId;
    }
    
    /**
     * 获取会话中的用户
     */
    public User getUser(String sessionId) {
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            return null;
        }
        return sessions.get(sessionId);
    }
    
    /**
     * 移除会话（登出）
     */
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }
}
