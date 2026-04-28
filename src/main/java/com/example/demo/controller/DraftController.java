package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.dto.DraftDto;
import com.example.demo.entity.Draft;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.service.DraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;
    private final SessionManager sessionManager;

    private User requireLogin(String sessionId) {
        User user = sessionManager.getUser(sessionId);
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, 401, "请先登录");
        }
        return user;
    }

    /**
     * 保存草稿
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveDraft(@RequestBody DraftDto draftDto,
                                                         @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        User user = requireLogin(sessionId);
        Draft savedDraft = draftService.saveDraft(draftDto, user.getId());
        result.put("code", 200);
        result.put("message", "保存成功");
        result.put("data", savedDraft);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有草稿列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllDrafts(@CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        User user = requireLogin(sessionId);
        List<Draft> drafts = draftService.getAllDrafts(user.getId());
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", drafts);
        return ResponseEntity.ok(result);
    }

    /**
     * 根据ID获取草稿详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDraftById(@PathVariable Long id,
                                                            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        User user = requireLogin(sessionId);
        Draft draft = draftService.getDraftById(id, user.getId());
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", draft);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除草稿
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDraft(@PathVariable Long id,
                                                           @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        User user = requireLogin(sessionId);
        draftService.deleteDraft(id, user.getId());
        result.put("code", 200);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteDraft(@RequestBody DraftDto draftDto,
                                                                @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> result = new HashMap<>();
        User user = requireLogin(sessionId);
        draftService.batchDeleteDrafts(draftDto.getIds(), user.getId());
        result.put("code", 200);
        result.put("message", "批量删除成功");
        return ResponseEntity.ok(result);
    }
}
