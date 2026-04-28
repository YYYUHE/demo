package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.dto.CommentDto;
import com.example.demo.dto.CommentPageDto;
import com.example.demo.dto.CreateCommentRequest;
import com.example.demo.entity.User;
import com.example.demo.service.CommentService;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final SessionManager sessionManager;
    private final NotificationService notificationService;

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Map<String, Object>> listComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "time") String sort,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            User user = sessionId == null ? null : sessionManager.getUser(sessionId);
            Long userId = user == null ? null : user.getId();
            CommentPageDto data = commentService.getCommentPage(postId, page, size, sort, userId);
            resp.put("code", 200);
            resp.put("message", "获取成功");
            resp.put("data", data);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("code", 500);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    @GetMapping("/posts/{postId}/comments/{commentId}/replies")
    public ResponseEntity<Map<String, Object>> listReplies(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            User user = sessionId == null ? null : sessionManager.getUser(sessionId);
            Long userId = user == null ? null : user.getId();
            List<CommentDto> data = commentService.getReplies(postId, commentId, page, size, userId);
            resp.put("code", 200);
            resp.put("message", "获取成功");
            resp.put("data", data);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("code", 500);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable Long postId,
            @RequestBody CreateCommentRequest request,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            User user = sessionManager.getUser(sessionId);
            if (user == null) {
                resp.put("code", 401);
                resp.put("message", "请先登录");
                return ResponseEntity.status(401).body(resp);
            }
            CommentDto data = commentService.createComment(postId, request.getParentId(), request.getContent(), user);
            resp.put("code", 200);
            resp.put("message", "发表成功");
            resp.put("data", data);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("code", 400);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long commentId,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            User user = sessionManager.getUser(sessionId);
            if (user == null) {
                resp.put("code", 401);
                resp.put("message", "请先登录");
                return ResponseEntity.status(401).body(resp);
            }
            Map<String, Object> data = commentService.toggleLike(commentId, user);
            resp.put("code", 200);
            resp.put("message", "操作成功");
            resp.put("data", data);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("code", 400);
            resp.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/comments/{commentId}/favorite")
    public ResponseEntity<Map<String, Object>> favoriteComment(@PathVariable Long commentId,
                                                               @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        User user = sessionManager.getUser(sessionId);
        if (user == null) {
            resp.put("code", 401);
            resp.put("message", "请先登录");
            return ResponseEntity.status(401).body(resp);
        }
        commentService.favoriteComment(commentId, user);
        resp.put("code", 200);
        resp.put("message", "收藏成功");
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/comments/{commentId}/favorite")
    public ResponseEntity<Map<String, Object>> unfavoriteComment(@PathVariable Long commentId,
                                                                 @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        User user = sessionManager.getUser(sessionId);
        if (user == null) {
            resp.put("code", 401);
            resp.put("message", "请先登录");
            return ResponseEntity.status(401).body(resp);
        }
        commentService.unfavoriteComment(commentId, user);
        resp.put("code", 200);
        resp.put("message", "取消收藏成功");
        return ResponseEntity.ok(resp);
    }

    /**
     * 获取回复我的消息列表
     */
    @GetMapping("/messages/replies")
    public ResponseEntity<Map<String, Object>> getReplyMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            User user = sessionManager.getUser(sessionId);
            if (user == null) {
                resp.put("code", 401);
                resp.put("message", "请先登录");
                return ResponseEntity.status(401).body(resp);
            }
            var data = commentService.getReplyMessages(user.getId(), page, size);
            Map<String, Object> result = new HashMap<>();
            result.put("items", data.getContent());
            result.put("page", page);
            result.put("size", size);
            result.put("total", data.getTotalElements());
            result.put("hasMore", data.hasNext());
            resp.put("code", 200);
            resp.put("message", "获取成功");
            resp.put("data", result);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("code", 500);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    /**
     * 删除回复通知
     */
    @DeleteMapping("/messages/replies/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteReplyNotification(
            @PathVariable Long notificationId,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            User user = sessionManager.getUser(sessionId);
            if (user == null) {
                resp.put("code", 401);
                resp.put("message", "请先登录");
                return ResponseEntity.status(401).body(resp);
            }
            notificationService.deleteNotification(user.getId(), notificationId);
            resp.put("code", 200);
            resp.put("message", "删除成功");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("code", 500);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    /**
     * 获取我发布的帖子收到的评论消息列表（我的消息-帖子板块）
     */
    @GetMapping("/messages/post-comments")
    public ResponseEntity<Map<String, Object>> getPostCommentMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            User user = sessionManager.getUser(sessionId);
            if (user == null) {
                resp.put("code", 401);
                resp.put("message", "请先登录");
                return ResponseEntity.status(401).body(resp);
            }
            var data = commentService.getPostCommentMessages(user.getId(), page, size);
            Map<String, Object> result = new HashMap<>();
            result.put("items", data.getContent());
            result.put("page", page);
            result.put("size", size);
            result.put("total", data.getTotalElements());
            result.put("hasMore", data.hasNext());
            resp.put("code", 200);
            resp.put("message", "获取成功");
            resp.put("data", result);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("code", 500);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    /**
     * 获取收到的赞消息列表（合并5分钟内同一条内容的赞）
     */
    @GetMapping("/messages/likes-received")
    public ResponseEntity<Map<String, Object>> getLikesReceivedMessages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            User user = sessionManager.getUser(sessionId);
            if (user == null) {
                resp.put("code", 401);
                resp.put("message", "请先登录");
                return ResponseEntity.status(401).body(resp);
            }
            var data = commentService.getLikesReceivedMessages(user.getId(), page, size);
            Map<String, Object> result = new HashMap<>();
            result.put("items", data.getContent());
            result.put("page", page);
            result.put("size", size);
            result.put("total", data.getTotalElements());
            result.put("hasMore", data.hasNext());
            resp.put("code", 200);
            resp.put("message", "获取成功");
            resp.put("data", result);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("code", 500);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }
}

