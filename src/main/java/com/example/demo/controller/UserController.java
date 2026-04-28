package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    @GetMapping("/basic")
    public ResponseEntity<Map<String, Object>> getUsersBasic(
            @CookieValue(value = "sessionId", required = false) String sessionId,
            @RequestParam List<Long> ids) {
        Map<String, Object> result = new HashMap<>();

        User currentUser = sessionManager.getUser(sessionId);
        if (currentUser == null) {
            result.put("code", 401);
            result.put("message", "请先登录");
            return ResponseEntity.status(401).body(result);
        }

        if (ids == null || ids.isEmpty()) {
            result.put("code", 200);
            result.put("message", "获取成功");
            result.put("data", List.of());
            return ResponseEntity.ok(result);
        }

        List<User> users = userRepository.findAllById(ids);
        List<Map<String, Object>> data = users.stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "avatar", user.getAvatar() != null ? user.getAvatar() : ""
                ))
                .collect(Collectors.toList());

        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", data);
        return ResponseEntity.ok(result);
    }
}
