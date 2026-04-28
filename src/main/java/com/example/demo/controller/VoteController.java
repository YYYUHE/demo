package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.dto.CastVoteRequest;
import com.example.demo.dto.CreateVoteRequest;
import com.example.demo.dto.VoteDto;
import com.example.demo.entity.User;
import com.example.demo.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final SessionManager sessionManager;

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 创建投票
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createVote(
            @RequestParam Long postId,
            @RequestBody CreateVoteRequest request,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取当前登录用户
            User currentUser = sessionManager.getUser(sessionId);
            if (currentUser == null) {
                response.put("code", 401);
                response.put("message", "请先登录");
                return ResponseEntity.status(401).body(response);
            }

            VoteDto voteDto = voteService.createVote(postId, request);

            response.put("code", 200);
            response.put("message", "创建成功");
            response.put("data", voteDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "创建失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 获取帖子的投票信息
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<Map<String, Object>> getVoteByPostId(
            @PathVariable Long postId,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = sessionManager.getUser(sessionId);
            VoteDto voteDto = voteService.getVoteByPostId(postId, currentUser);

            if (voteDto == null) {
                response.put("code", 404);
                response.put("message", "该帖子没有投票");
                return ResponseEntity.status(404).body(response);
            }

            response.put("code", 200);
            response.put("data", voteDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "查询失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据投票ID获取投票信息
     */
    @GetMapping("/{voteId}")
    public ResponseEntity<Map<String, Object>> getVoteById(
            @PathVariable Long voteId,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            User currentUser = sessionManager.getUser(sessionId);
            VoteDto voteDto = voteService.getVoteById(voteId, currentUser);

            response.put("code", 200);
            response.put("data", voteDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "查询失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 提交投票
     */
    @PostMapping("/cast")
    public ResponseEntity<Map<String, Object>> castVote(
            @RequestBody CastVoteRequest request,
            @CookieValue(value = "sessionId", required = false) String sessionId,
            HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取当前登录用户
            User currentUser = sessionManager.getUser(sessionId);
            if (currentUser == null) {
                response.put("code", 401);
                response.put("message", "请先登录");
                return ResponseEntity.status(401).body(response);
            }

            String ip = getClientIp(httpRequest);
            voteService.castVote(request, currentUser, ip);

            response.put("code", 200);
            response.put("message", "投票成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "投票失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 删除投票
     */
    @DeleteMapping("/{voteId}")
    public ResponseEntity<Map<String, Object>> deleteVote(
            @PathVariable Long voteId,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取当前登录用户
            User currentUser = sessionManager.getUser(sessionId);
            if (currentUser == null) {
                response.put("code", 401);
                response.put("message", "请先登录");
                return ResponseEntity.status(401).body(response);
            }

            voteService.deleteVote(voteId);

            response.put("code", 200);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", "删除失败：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
