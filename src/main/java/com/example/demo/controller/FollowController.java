package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.entity.User;
import com.example.demo.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关注功能控制器
 */
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;
    private final SessionManager sessionManager;

    /**
     * 切换关注状态（关注/取消关注）
     */
    @PostMapping("/{userId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleFollow(
            @PathVariable Long userId,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = sessionManager.getUser(sessionId);
            if (currentUser == null) {
                response.put("code", 401);
                response.put("message", "请先登录");
                return ResponseEntity.status(401).body(response);
            }

            Map<String, Object> result = followService.toggleFollow(currentUser.getId(), userId);
            
            response.put("code", 200);
            response.put("message", "操作成功");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 检查是否已关注某用户
     */
    @GetMapping("/{userId}/check")
    public ResponseEntity<Map<String, Object>> checkFollowStatus(
            @PathVariable Long userId,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = sessionManager.getUser(sessionId);
            if (currentUser == null) {
                response.put("code", 200);
                response.put("data", Map.of("following", false));
                return ResponseEntity.ok(response);
            }

            boolean following = followService.isFollowing(currentUser.getId(), userId);
            long followerCount = followService.getFollowerCount(userId);

            response.put("code", 200);
            response.put("data", Map.of(
                    "following", following,
                    "followerCount", followerCount
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取当前用户的关注列表
     */
    @GetMapping("/my-following")
    public ResponseEntity<Map<String, Object>> getMyFollowing(
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = sessionManager.getUser(sessionId);
            if (currentUser == null) {
                response.put("code", 401);
                response.put("message", "请先登录");
                return ResponseEntity.status(401).body(response);
            }

            List<Map<String, Object>> followingUsers = followService.getFollowingUsersWithDetails(
                    currentUser.getId(), currentUser.getId());

            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", followingUsers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取某用户的关注列表
     */
    @GetMapping("/user/{userId}/following")
    public ResponseEntity<Map<String, Object>> getUserFollowing(
            @PathVariable Long userId,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = sessionManager.getUser(sessionId);
            Long currentUserId = currentUser != null ? currentUser.getId() : null;

            List<Map<String, Object>> followingUsers = followService.getFollowingUsersWithDetails(
                    userId, currentUserId);

            response.put("code", 200);
            response.put("message", "获取成功");
            response.put("data", followingUsers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取关注统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFollowStats(
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = sessionManager.getUser(sessionId);
            if (currentUser == null) {
                response.put("code", 401);
                response.put("message", "请先登录");
                return ResponseEntity.status(401).body(response);
            }

            long followingCount = followService.getFollowingCount(currentUser.getId());
            long followerCount = followService.getFollowerCount(currentUser.getId());

            response.put("code", 200);
            response.put("data", Map.of(
                    "followingCount", followingCount,
                    "followerCount", followerCount
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
