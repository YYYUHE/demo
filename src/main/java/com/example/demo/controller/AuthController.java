package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final SessionManager sessionManager;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody UserDto userDto) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.register(userDto);
            
            response.put("code", 200);
            response.put("message", "注册成功");
            response.put("data", Map.of(
                "id", user.getId(),
                "uid", user.getUid(),
                "username", user.getUsername()
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody UserDto userDto, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            User user = userService.login(userDto);
            
            // 创建会话
            String sessionId = sessionManager.createSession(user);
            
            // 设置 Cookie
            Cookie cookie = new Cookie("sessionId", sessionId);
            cookie.setPath("/");
            cookie.setMaxAge(30 * 60); // 30分钟
            response.addCookie(cookie);
            
            result.put("code", 200);
            result.put("message", "登录成功");
            result.put("data", Map.of(
                "id", user.getId(),
                "uid", user.getUid(),
                "username", user.getUsername()
            ));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("code", 401);
            result.put("message", e.getMessage());
            return ResponseEntity.status(401).body(result);
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@CookieValue(value = "sessionId", required = false) String sessionId,
                                                       HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }
        
        // 清除 Cookie
        Cookie cookie = new Cookie("sessionId", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        result.put("code", 200);
        result.put("message", "登出成功");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查登录状态
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkLogin(@CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        
        if (sessionId != null) {
            User user = sessionManager.getUser(sessionId);
            if (user != null) {
                result.put("code", 200);
                result.put("message", "已登录");
                result.put("data", Map.of(
                    "id", user.getId(),
                    "uid", user.getUid(),
                    "username", user.getUsername()
                ));
                return ResponseEntity.ok(result);
            }
        }
        
        result.put("code", 401);
        result.put("message", "未登录");
        return ResponseEntity.status(401).body(result);
    }
}
