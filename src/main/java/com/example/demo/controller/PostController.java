package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.dto.*;
import com.example.demo.entity.User;
import com.example.demo.enums.PostCategory;
import com.example.demo.exception.ApiException;
import com.example.demo.service.PostService;
import com.example.demo.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final SessionManager sessionManager;
    private final VoteService voteService;
    private static final Set<String> CATEGORY_WHITELIST = PostCategory.whitelist();

    private User requireLogin(String sessionId) {
        User currentUser = sessionManager.getUser(sessionId);
        if (currentUser == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, 401, "请先登录");
        }
        return currentUser;
    }

    private boolean isAdmin(User user) {
        return user != null && "admin".equalsIgnoreCase(user.getUsername());
    }

    /**
     * 发布帖子
     */
    @PostMapping("/publish")
    public ResponseEntity<Map<String, Object>> publishPost(@RequestBody PostDto postDto, 
                                                           @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();
        if (postDto.getTitle() == null || postDto.getTitle().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 400, "标题不能为空");
        }
        if (postDto.getContent() == null || postDto.getContent().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 400, "内容不能为空");
        }
        if (postDto.getCategory() == null || !CATEGORY_WHITELIST.contains(postDto.getCategory().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 400, "不支持的帖子分类");
        }

        User currentUser = requireLogin(sessionId);

        postDto.setAuthorId(currentUser.getId());
        postDto.setAuthorUsername(currentUser.getUsername());
        postDto.setAuthorAvatar(currentUser.getAvatar());

        var post = postService.publishPost(postDto);

        if (postDto.getVote() != null && postDto.getVote().getOptions() != null && !postDto.getVote().getOptions().isEmpty()) {
            try {
                System.out.println("📊 开始创建投票，帖子ID: " + post.getId());
                CreateVoteRequest voteRequest = new CreateVoteRequest();
                voteRequest.setTitle(postDto.getVote().getTitle());
                voteRequest.setDeadline(postDto.getVote().getDeadline());
                voteRequest.setMaxChoices(postDto.getVote().getMaxChoices());
                voteRequest.setOptions(postDto.getVote().getOptions().stream()
                        .map(option -> option.getContent())
                        .collect(java.util.stream.Collectors.toList()));
                voteService.createVote(post.getId(), voteRequest);
                // 重新加载帖子以获取最新的voteId
                post = postService.getPostById(post.getId());
                System.out.println("✅ 投票创建成功，帖子ID: " + post.getId() + ", 投票ID: " + post.getVoteId());
            } catch (Exception e) {
                System.err.println("❌ 创建投票失败，帖子ID: " + post.getId() + ", 错误: " + e.getMessage());
                e.printStackTrace();
                response.put("code", 500);
                response.put("message", "帖子发布成功，但投票创建失败：" + e.getMessage());
                response.put("data", post);
                return ResponseEntity.status(500).body(response);
            }
        } else {
            System.out.println("ℹ️ 帖子没有投票数据，帖子ID: " + post.getId());
        }
        response.put("code", 200);
        response.put("message", "发布成功");
        response.put("data", post);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/anonymous/cancel")
    public ResponseEntity<Map<String, Object>> cancelAnonymous(@PathVariable Long id,
                                                               @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();
        User user = requireLogin(sessionId);
        postService.cancelAnonymous(id, user.getId());
        response.put("code", 200);
        response.put("message", "已取消匿名");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Map<String, Object>> favoritePost(@PathVariable Long id,
                                                            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();
        User user = requireLogin(sessionId);
        postService.favoritePost(id, user.getId());
        response.put("code", 200);
        response.put("message", "收藏成功");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<Map<String, Object>> unfavoritePost(@PathVariable Long id,
                                                              @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();
        User user = requireLogin(sessionId);
        postService.unfavoritePost(id, user.getId());
        response.put("code", 200);
        response.put("message", "取消收藏成功");
        return ResponseEntity.ok(response);
    }

    /**
     * 切换帖子点赞状态
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long id,
                                                          @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();
        User user = requireLogin(sessionId);
        boolean liked = postService.toggleLike(id, user.getId());
        long likeCount = postService.getLikeCount(id);
        response.put("code", 200);
        response.put("message", liked ? "点赞成功" : "取消点赞成功");
        Map<String, Object> data = new HashMap<>();
        data.put("liked", liked);
        data.put("likeCount", likeCount);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有帖子列表（支持分类筛选和搜索+分页）
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = sessionManager.getUser(sessionId);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50));
        Page<PostDetailDto> postPage = postService.listPosts(category, search, pageable, currentUser);
        Map<String, Object> data = new HashMap<>();
        data.put("items", postPage.getContent());
        data.put("page", page);
        data.put("size", size);
        data.put("total", postPage.getTotalElements());
        data.put("hasMore", postPage.hasNext());
        response.put("code", 200);
        response.put("message", "获取成功");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户的帖子列表
     */
    @GetMapping("/my-posts")
    public ResponseEntity<Map<String, Object>> getMyPosts(@CookieValue(value = "sessionId", required = false) String sessionId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = requireLogin(sessionId);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50));
        Page<PostDetailDto> postPage = postService.listMyPosts(currentUser.getId(), pageable, currentUser);
        Map<String, Object> data = new HashMap<>();
        data.put("items", postPage.getContent());
        data.put("page", page);
        data.put("size", size);
        data.put("total", postPage.getTotalElements());
        data.put("hasMore", postPage.hasNext());
        response.put("code", 200);
        response.put("message", "获取成功");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户的收藏列表
     */
    @GetMapping("/favorites")
    public ResponseEntity<Map<String, Object>> getFavoritePosts(
            @CookieValue(value = "sessionId", required = false) String sessionId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = requireLogin(sessionId);
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 50));
        Page<PostDetailDto> postPage = postService.listFavoritePosts(currentUser.getId(), category, keyword, pageable, currentUser);
        Map<String, Object> data = new HashMap<>();
        data.put("content", postPage.getContent());
        data.put("page", page);
        data.put("size", size);
        data.put("totalElements", postPage.getTotalElements());
        data.put("totalPages", postPage.getTotalPages());
        data.put("first", postPage.isFirst());
        data.put("last", postPage.isLast());
        data.put("hasMore", postPage.hasNext());
        response.put("code", 200);
        response.put("message", "获取成功");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取帖子详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPostById(@PathVariable Long id,
                                                            @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = sessionManager.getUser(sessionId);
        PostDetailDto postDetail = postService.getPostDetailById(id, currentUser);
        response.put("code", 200);
        response.put("message", "获取成功");
        response.put("data", postDetail);
        return ResponseEntity.ok(response);
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePost(@PathVariable Long id,
                                                          @CookieValue(value = "sessionId", required = false) String sessionId) {
        Map<String, Object> response = new HashMap<>();
        User currentUser = requireLogin(sessionId);
        postService.deletePost(id, currentUser.getId());
        response.put("code", 200);
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> categories() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "获取成功");
        response.put("data", CATEGORY_WHITELIST);
        return ResponseEntity.ok(response);
    }
}
