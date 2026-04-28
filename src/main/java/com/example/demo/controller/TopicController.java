package com.example.demo.controller;

import com.example.demo.dto.TopicDto;
import com.example.demo.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 话题控制器
 */
@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {
    
    private final TopicService topicService;
    
    /**
     * 创建新话题
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTopic(
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String name = request.get("name");
            String description = request.getOrDefault("description", "");
            
            TopicDto topicDto = topicService.createTopic(name, description);
            
            response.put("code", 200);
            response.put("message", "话题创建成功");
            response.put("data", topicDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取或创建话题
     */
    @PostMapping("/get-or-create")
    public ResponseEntity<Map<String, Object>> getOrCreateTopic(
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String name = request.get("name");
            TopicDto topicDto = topicService.getOrCreateTopic(name);
            
            response.put("code", 200);
            response.put("data", topicDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 根据ID获取话题
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTopicById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            TopicDto topicDto = topicService.getTopicById(id);
            
            response.put("code", 200);
            response.put("data", topicDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 404);
            response.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 搜索话题（模糊匹配）
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTopics(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TopicDto> topics = topicService.searchTopics(keyword);
            
            response.put("code", 200);
            response.put("data", topics);
            response.put("count", topics.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取热门话题
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularTopics(
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TopicDto> topics = topicService.getPopularTopics(limit);
            
            response.put("code", 200);
            response.put("data", topics);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取搜索建议（前缀匹配）
     */
    @GetMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> getSuggestions(
            @RequestParam String prefix,
            @RequestParam(defaultValue = "5") int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TopicDto> suggestions = topicService.getSuggestions(prefix, limit);
            
            response.put("code", 200);
            response.put("data", suggestions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 增加话题使用次数
     */
    @PostMapping("/{topicName}/increment-usage")
    public ResponseEntity<Map<String, Object>> incrementUsage(@PathVariable String topicName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            topicService.incrementUsage(topicName);
            
            response.put("code", 200);
            response.put("message", "使用次数已更新");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 500);
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 删除话题
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTopic(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            topicService.deleteTopic(id);
            
            response.put("code", 200);
            response.put("message", "话题删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
