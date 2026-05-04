package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/basic")
    public ResponseEntity<Map<String, Object>> getUsersBasic(@RequestParam String ids) {
        Map<String, Object> result = new HashMap<>();

        List<Long> idList;
        try {
            idList = List.of(ids.split(",")).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            result.put("code", 400);
            result.put("message", "无效的用户ID格式");
            return ResponseEntity.badRequest().body(result);
        }

        List<Map<String, Object>> users = userRepository.findAllById(idList).stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
                    return map;
                })
                .collect(Collectors.toList());

        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", users);
        return ResponseEntity.ok(result);
    }
}
