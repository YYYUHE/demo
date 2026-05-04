package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.dto.MentionDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.service.MentionService;
import com.example.demo.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mentions")
@RequiredArgsConstructor
public class MentionController {
    
    private final MentionService mentionService;
    private final UserSearchService userSearchService;
    private final SessionManager sessionManager;
    
    /**
     * 搜索关注的用户（用于@功能）
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @CookieValue(value = "sessionId", required = false) String sessionId,
            @RequestParam(required = false) String keyword) {
        
        Map<String, Object> result = new HashMap<>();
        User currentUser = sessionManager.getUser(sessionId);
        
        if (currentUser == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return ResponseEntity.status(401).body(result);
        }
        
        List<UserDto> users = userSearchService.searchFollowingUsers(currentUser.getId(), keyword);
        
        result.put("code", 200);
        result.put("message", "搜索成功");
        result.put("data", users);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取@我的列表（兼容 /api/messages/mentions 路径）
     */
    @GetMapping({"/me", "/messages/mentions"})
    public ResponseEntity<Map<String, Object>> getMyMentions(
            @CookieValue(value = "sessionId", required = false) String sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Map<String, Object> result = new HashMap<>();
        User currentUser = sessionManager.getUser(sessionId);
        
        if (currentUser == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return ResponseEntity.status(401).body(result);
        }
        
        var pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(size, 50));
        Page<MentionDto> mentions = mentionService.getMyMentions(currentUser.getId(), pageable);
        
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", Map.of(
                "content", mentions.getContent(),
                "page", page,
                "size", size,
                "totalElements", mentions.getTotalElements(),
                "totalPages", mentions.getTotalPages(),
                "hasMore", mentions.hasNext()
        ));
        return ResponseEntity.ok(result);
    }
    
    /**
     * 标记@通知为已读
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long id,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        
        Map<String, Object> result = new HashMap<>();
        User currentUser = sessionManager.getUser(sessionId);
        
        if (currentUser == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return ResponseEntity.status(401).body(result);
        }
        
        try {
            mentionService.markAsRead(id, currentUser.getId());
            result.put("code", 200);
            result.put("message", "操作成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 删除@通知
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMention(
            @PathVariable Long id,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        
        Map<String, Object> result = new HashMap<>();
        User currentUser = sessionManager.getUser(sessionId);
        
        if (currentUser == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return ResponseEntity.status(401).body(result);
        }
        
        try {
            mentionService.deleteMention(id, currentUser.getId());
            result.put("code", 200);
            result.put("message", "删除成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("code", 400);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 获取未读@数量
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        
        Map<String, Object> result = new HashMap<>();
        User currentUser = sessionManager.getUser(sessionId);
        
        if (currentUser == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return ResponseEntity.status(401).body(result);
        }
        
        long count = mentionService.getUnreadCount(currentUser.getId());
        
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", Map.of("count", count));
        return ResponseEntity.ok(result);
    }
}
