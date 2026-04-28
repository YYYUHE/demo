package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.entity.User;
import com.example.demo.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class UserProfileController {
    
    private final UserProfileService userProfileService;
    private final SessionManager sessionManager;
    
    /**
     * 获取当前用户完整信息
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUserProfile(@CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        
        if (sessionId == null) {
            result.put("code", 401);
            result.put("message", "未登录");
            return ResponseEntity.status(401).body(result);
        }
        
        User user = sessionManager.getUser(sessionId);
        if (user == null) {
            result.put("code", 401);
            result.put("message", "会话已过期");
            return ResponseEntity.status(401).body(result);
        }
        
        // 从数据库获取最新用户信息（包含头像）
        User latestUser = userProfileService.getUserProfile(user.getId());
        
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", Map.of(
            "id", latestUser.getId(),
            "uid", latestUser.getUid(),
            "username", latestUser.getUsername(),
            "avatar", latestUser.getAvatar() != null ? latestUser.getAvatar() : "",
            "createTime", latestUser.getCreateTime().toString()
        ));
        return ResponseEntity.ok(result);
    }
    
    /**
     * 更新用户头像
     */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(
            @CookieValue(value = "sessionId", required = false) String sessionId,
            @RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        
        if (sessionId == null) {
            result.put("code", 401);
            result.put("message", "未登录");
            return ResponseEntity.status(401).body(result);
        }
        
        User user = sessionManager.getUser(sessionId);
        if (user == null) {
            result.put("code", 401);
            result.put("message", "会话已过期");
            return ResponseEntity.status(401).body(result);
        }
        
        try {
            String avatarBase64 = request.get("avatar");
            User updatedUser = userProfileService.updateAvatar(user.getId(), avatarBase64);
            
            result.put("code", 200);
            result.put("message", "头像更新成功");
            result.put("data", Map.of(
                "avatar", updatedUser.getAvatar() != null ? updatedUser.getAvatar() : ""
            ));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
