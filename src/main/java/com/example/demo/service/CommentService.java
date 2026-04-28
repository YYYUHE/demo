package com.example.demo.service;

import com.example.demo.dto.CommentDto;
import com.example.demo.dto.CommentPageDto;
import com.example.demo.entity.Comment;
import com.example.demo.entity.CommentFavorite;
import com.example.demo.entity.CommentLike;
import com.example.demo.entity.Notification;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.CommentFavoriteRepository;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int DEFAULT_REPLY_PREVIEW_SIZE = 3;
    private static final int DEFAULT_THIRD_PREVIEW_SIZE = 3;

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentFavoriteRepository commentFavoriteRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final MentionService mentionService;

    public CommentPageDto getCommentPage(Long postId, int page, int size, String sort, Long currentUserId) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Sort pageSort;
        if ("hot".equalsIgnoreCase(sort)) {
            pageSort = Sort.by(Sort.Order.desc("likeCount"), Sort.Order.desc("createTime"));
        } else {
            pageSort = Sort.by(Sort.Order.desc("createTime"));
        }

        Pageable pageable = PageRequest.of(safePage - 1, safeSize, pageSort);
        Page<Comment> topPage = commentRepository.findByPostIdAndDepth(postId, 1, pageable);
        List<Comment> topComments = topPage.getContent();

        Map<Long, List<Comment>> secondMap = new HashMap<>();
        Map<Long, Long> secondCountMap = new HashMap<>();
        for (Comment top : topComments) {
            long secondCount = commentRepository.countByRootIdAndDepth(top.getId(), 2);
            secondCountMap.put(top.getId(), secondCount);
            Pageable replyPage = PageRequest.of(0, DEFAULT_REPLY_PREVIEW_SIZE, Sort.by(Sort.Order.asc("createTime")));
            List<Comment> seconds = commentRepository.findByRootIdAndDepth(top.getId(), 2, replyPage);
            secondMap.put(top.getId(), seconds);
        }

        Map<Long, List<Comment>> thirdMap = new HashMap<>();
        Map<Long, Long> thirdCountMap = new HashMap<>();
        for (List<Comment> seconds : secondMap.values()) {
            for (Comment second : seconds) {
                long thirdCount = commentRepository.countByParentId(second.getId());
                thirdCountMap.put(second.getId(), thirdCount);
                Pageable thirdPage = PageRequest.of(0, DEFAULT_THIRD_PREVIEW_SIZE, Sort.by(Sort.Order.asc("createTime")));
                List<Comment> thirds = commentRepository.findByParentId(second.getId(), thirdPage);
                thirdMap.put(second.getId(), thirds);
            }
        }

        Set<Long> likedIds = loadLikedCommentIds(currentUserId, collectAllIds(topComments, secondMap, thirdMap));
        Set<Long> favoritedIds = loadFavoritedCommentIds(currentUserId, collectAllIds(topComments, secondMap, thirdMap));

        List<CommentDto> items = new ArrayList<>();
        for (Comment top : topComments) {
            List<CommentDto> secondDtos = new ArrayList<>();
            List<Comment> seconds = secondMap.getOrDefault(top.getId(), List.of());
            for (Comment second : seconds) {
                List<CommentDto> thirdDtos = new ArrayList<>();
                List<Comment> thirds = thirdMap.getOrDefault(second.getId(), List.of());
                for (Comment third : thirds) {
                    thirdDtos.add(toDto(third, likedIds.contains(third.getId()), favoritedIds.contains(third.getId()), 0L, List.of()));
                }
                long thirdCount = thirdCountMap.getOrDefault(second.getId(), 0L);
                secondDtos.add(toDto(second, likedIds.contains(second.getId()), favoritedIds.contains(second.getId()), thirdCount, thirdDtos));
            }
            long secondCount = secondCountMap.getOrDefault(top.getId(), 0L);
            items.add(toDto(top, likedIds.contains(top.getId()), favoritedIds.contains(top.getId()), secondCount, secondDtos));
        }

        return CommentPageDto.builder()
                .page(safePage)
                .size(safeSize)
                .total(topPage.getTotalElements())
                .items(items)
                .build();
    }

    public List<CommentDto> getReplies(Long postId, Long commentId, int page, int size, Long currentUserId) {
        Comment parent = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.asc("createTime")));

        List<Comment> children;
        if (parent.getDepth() == 1) {
            children = commentRepository.findByRootIdAndDepth(parent.getId(), 2, pageable);
        } else if (parent.getDepth() == 2) {
            children = commentRepository.findByParentId(parent.getId(), pageable);
        } else {
            return List.of();
        }

        Set<Long> likedIds = loadLikedCommentIds(currentUserId, children.stream().map(Comment::getId).toList());
        Set<Long> favoritedIds = loadFavoritedCommentIds(currentUserId, children.stream().map(Comment::getId).toList());
        List<CommentDto> result = new ArrayList<>();
        for (Comment c : children) {
            long replyCount = c.getDepth() == 2 ? commentRepository.countByParentId(c.getId()) : 0L;
            result.add(toDto(c, likedIds.contains(c.getId()), favoritedIds.contains(c.getId()), replyCount, List.of()));
        }
        return result;
    }

    @Transactional
    public CommentDto createComment(Long postId, Long parentId, String content, User user) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new RuntimeException("评论内容不能为空");
        }
        if (text.length() > 500) {
            throw new RuntimeException("评论内容最多500字");
        }

        if (parentId == null) {
            Comment saved = commentRepository.save(Comment.builder()
                    .postId(postId)
                    .parentId(null)
                    .rootId(null)
                    .depth(1)
                    .content(text)
                    .likeCount(0L)
                    .authorId(user.getId())
                    .authorUsername(user.getUsername())
                    .authorAvatar(user.getAvatar())
                    .build());
            saved.setRootId(saved.getId());
            Comment updated = commentRepository.save(saved);
                    
            // 处理@提及
            mentionService.createMentions(postId, updated.getId(), text, user.getId());
                    
            return toDto(updated, false, false, 0L, List.of());
        }

        Comment parent = commentRepository.findByIdAndPostId(parentId, postId)
                .orElseThrow(() -> new RuntimeException("回复的评论不存在"));
        if (parent.getDepth() >= 3) {
            throw new RuntimeException("最多支持三级回复");
        }

        int depth = parent.getDepth() + 1;
        Long rootId = parent.getDepth() == 1 ? parent.getId() : parent.getRootId();
        Comment saved = commentRepository.save(Comment.builder()
                .postId(postId)
                .parentId(parent.getId())
                .rootId(rootId)
                .depth(depth)
                .content(text)
                .likeCount(0L)
                .authorId(user.getId())
                .authorUsername(user.getUsername())
                .authorAvatar(user.getAvatar())
                .build());
        
        // 处理@提及
        mentionService.createMentions(postId, saved.getId(), text, user.getId());

        // 如果是回复别人的评论（depth >= 2），且不是自己回复自己，则创建通知
        if (depth >= 2 && !parent.getAuthorId().equals(user.getId())) {
            var post = postRepository.findById(postId);
            if (post.isPresent()) {
                notificationService.createReplyNotification(
                        parent.getAuthorId(),
                        postId,
                        parent.getId(),
                        saved.getId(),
                        user.getId(),
                        user.getUsername(),
                        user.getAvatar(),
                        text,
                        parent.getContent(),
                        post.get().getTitle()
                );
            }
        }

        return toDto(saved, false, false, 0L, List.of());
    }

    @Transactional
    public Map<String, Object> toggleLike(Long commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        boolean liked;
        CommentLike existing = commentLikeRepository.findByCommentIdAndUserId(commentId, user.getId()).orElse(null);
        if (existing != null) {
            commentLikeRepository.delete(existing);
            long next = Math.max(0L, comment.getLikeCount() - 1);
            comment.setLikeCount(next);
            liked = false;
        } else {
            commentLikeRepository.save(CommentLike.builder()
                    .commentId(commentId)
                    .userId(user.getId())
                    .build());
            comment.setLikeCount(comment.getLikeCount() + 1);
            liked = true;
            
            // 创建点赞通知（如果不是自己给自己点赞）
            if (!comment.getAuthorId().equals(user.getId())) {
                var post = postRepository.findById(comment.getPostId());
                if (post.isPresent()) {
                    // 确定目标类型：depth=1为评论，depth>=2为回复
                    String targetType = comment.getDepth() >= 2 ? "reply" : "comment";
                    notificationService.createLikeNotification(
                            comment.getAuthorId(),
                            comment.getPostId(),
                            comment.getDepth() == 1 ? comment.getId() : comment.getParentId(),
                            comment.getDepth() >= 2 ? comment.getId() : null,
                            targetType,
                            user.getId(),
                            user.getUsername(),
                            user.getAvatar(),
                            comment.getContent(),
                            post.get().getTitle()
                    );
                }
            }
        }
        commentRepository.save(comment);

        Map<String, Object> result = new HashMap<>();
        result.put("likeCount", comment.getLikeCount());
        result.put("liked", liked);
        return result;
    }

    @Transactional
    public void favoriteComment(Long commentId, User user) {
        commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("评论不存在"));
        commentFavoriteRepository.findByCommentIdAndUserId(commentId, user.getId()).orElseGet(() ->
                commentFavoriteRepository.save(CommentFavorite.builder().commentId(commentId).userId(user.getId()).build()));
    }

    @Transactional
    public void unfavoriteComment(Long commentId, User user) {
        commentFavoriteRepository.findByCommentIdAndUserId(commentId, user.getId()).ifPresent(commentFavoriteRepository::delete);
    }

    /**
     * 获取回复我的消息列表
     * @param userId 当前用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 回复消息列表
     */
    public Page<Map<String, Object>> getReplyMessages(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.desc("createTime")));
        
        // 从通知表查询回复消息
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, "reply", pageable);
        
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Notification notification : notificationPage.getContent()) {
            Map<String, Object> message = new HashMap<>();
            message.put("id", notification.getId());
            message.put("notificationId", notification.getId());
            message.put("postId", notification.getPostId());
            message.put("postTitle", notification.getPostTitle());
            message.put("replyId", notification.getReplyId());
            message.put("parentId", notification.getCommentId());
            message.put("parentContent", notification.getParentContent());
            message.put("replyContent", notification.getContent());
            message.put("replyAuthorId", notification.getSenderId());
            message.put("replyAuthorUsername", notification.getSenderUsername());
            message.put("replyAuthorAvatar", notification.getSenderAvatar());
            message.put("createTime", notification.getCreateTime());
            message.put("isRead", notification.getIsRead());
            
            messages.add(message);
        }
        
        return new org.springframework.data.domain.PageImpl<>(messages, pageable, notificationPage.getTotalElements());
    }

    /**
     * 获取我发布的帖子收到的评论消息列表
     * @param userId 当前用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 帖子评论消息列表
     */
    public Page<Map<String, Object>> getPostCommentMessages(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.desc("createTime")));
        
        // 查找用户发布的所有帖子
        List<Post> userPosts = postRepository.findByAuthorIdOrderByCreateTimeDesc(userId, 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        
        if (userPosts.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        
        List<Long> postIds = userPosts.stream().map(Post::getId).collect(java.util.stream.Collectors.toList());
        
        // 查询这些帖子的所有一级评论（depth=1），排除自己的评论
        Page<Comment> commentPage = commentRepository.findByPostIdInAndDepthAndAuthorIdNotOrderByCreateTimeDesc(
                postIds, 1, userId, pageable);
        
        List<Map<String, Object>> messages = new ArrayList<>();
        for (Comment comment : commentPage.getContent()) {
            Map<String, Object> message = new HashMap<>();
            message.put("id", comment.getId());
            message.put("commentId", comment.getId());
            message.put("postId", comment.getPostId());
            
            // 获取帖子标题
            String postTitle = userPosts.stream()
                    .filter(p -> p.getId().equals(comment.getPostId()))
                    .map(Post::getTitle)
                    .findFirst()
                    .orElse("未知帖子");
            message.put("postTitle", postTitle);
            
            message.put("commentContent", comment.getContent());
            message.put("commentAuthorId", comment.getAuthorId());
            message.put("commentAuthorUsername", comment.getAuthorUsername());
            message.put("commentAuthorAvatar", comment.getAuthorAvatar());
            message.put("createTime", comment.getCreateTime());
            
            messages.add(message);
        }
        
        return new org.springframework.data.domain.PageImpl<>(messages, pageable, commentPage.getTotalElements());
    }

    /**
     * 获取收到的赞消息列表（合并5分钟内同一条内容的赞）
     * @param userId 当前用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 点赞消息列表
     */
    public Page<Map<String, Object>> getLikesReceivedMessages(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Order.desc("createTime")));
        
        // 从通知表查询点赞消息
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndTypeOrderByCreateTimeDesc(userId, "like", pageable);
        
        List<Notification> notifications = notificationPage.getContent();
        List<Map<String, Object>> messages = new ArrayList<>();
        
        // 合并5分钟内的同一条内容的赞
        java.time.LocalDateTime mergeWindow = java.time.LocalDateTime.now().minusMinutes(5);
        Map<String, Map<String, Object>> mergedMap = new java.util.LinkedHashMap<>();
        
        for (Notification notification : notifications) {
            // 生成合并键：targetType + targetId + 时间窗口
            String mergeKey;
            if ("post".equals(notification.getTargetType())) {
                mergeKey = "post_" + notification.getPostId();
            } else if ("comment".equals(notification.getTargetType())) {
                mergeKey = "comment_" + notification.getCommentId();
            } else if ("reply".equals(notification.getTargetType())) {
                mergeKey = "reply_" + notification.getReplyId();
            } else {
                continue;
            }
            
            // 如果在5分钟窗口内，则合并
            if (notification.getCreateTime().isAfter(mergeWindow) && mergedMap.containsKey(mergeKey)) {
                Map<String, Object> existing = mergedMap.get(mergeKey);
                existing.put("likeCount", (int) existing.get("likeCount") + 1);
                
                // 添加新的点赞者到列表
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> likers = (List<Map<String, Object>>) existing.get("likers");
                Map<String, Object> newLiker = new HashMap<>();
                newLiker.put("userId", notification.getSenderId());
                newLiker.put("username", notification.getSenderUsername());
                newLiker.put("avatar", notification.getSenderAvatar());
                likers.add(newLiker);
                
                // 更新最新点赞时间
                existing.put("latestTime", notification.getCreateTime());
            } else {
                // 创建新消息
                Map<String, Object> message = new HashMap<>();
                message.put("id", notification.getId());
                message.put("notificationId", notification.getId());
                message.put("postId", notification.getPostId());
                message.put("commentId", notification.getCommentId());
                message.put("replyId", notification.getReplyId());
                message.put("targetType", notification.getTargetType());
                message.put("postTitle", notification.getPostTitle());
                message.put("content", notification.getContent());
                message.put("likeCount", 1);
                message.put("latestTime", notification.getCreateTime());
                message.put("createTime", notification.getCreateTime());
                message.put("isRead", notification.getIsRead());
                
                List<Map<String, Object>> likers = new ArrayList<>();
                Map<String, Object> firstLiker = new HashMap<>();
                firstLiker.put("userId", notification.getSenderId());
                firstLiker.put("username", notification.getSenderUsername());
                firstLiker.put("avatar", notification.getSenderAvatar());
                likers.add(firstLiker);
                message.put("likers", likers);
                
                mergedMap.put(mergeKey, message);
            }
        }
        
        messages.addAll(mergedMap.values());
        
        // 按时间排序
        messages.sort((m1, m2) -> {
            java.time.LocalDateTime t1 = (java.time.LocalDateTime) m1.get("latestTime");
            java.time.LocalDateTime t2 = (java.time.LocalDateTime) m2.get("latestTime");
            return t2.compareTo(t1);
        });
        
        // 分页处理
        int total = messages.size();
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<Map<String, Object>> pagedMessages = messages.subList(fromIndex, toIndex);
        
        return new org.springframework.data.domain.PageImpl<>(pagedMessages, pageable, total);
    }

    private CommentDto toDto(Comment c, boolean liked, boolean favorited, long replyCount, List<CommentDto> replies) {
        // 从用户表获取最新的头像信息
        String latestAvatar = c.getAuthorAvatar();
        if (c.getAuthorId() != null) {
            latestAvatar = userRepository.findById(c.getAuthorId())
                    .map(User::getAvatar)
                    .orElse(c.getAuthorAvatar());
        }
        
        return CommentDto.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .parentId(c.getParentId())
                .rootId(c.getRootId())
                .depth(c.getDepth())
                .content(c.getContent())
                .likeCount(c.getLikeCount())
                .liked(liked)
                .favorited(favorited)
                .authorId(c.getAuthorId())
                .authorUsername(c.getAuthorUsername())
                .authorAvatar(latestAvatar)
                .createTime(c.getCreateTime())
                .replyCount(replyCount)
                .replies(replies)
                .build();
    }

    private List<Long> collectAllIds(List<Comment> top, Map<Long, List<Comment>> second, Map<Long, List<Comment>> third) {
        List<Long> ids = new ArrayList<>();
        top.forEach(c -> ids.add(c.getId()));
        second.values().forEach(list -> list.forEach(c -> ids.add(c.getId())));
        third.values().forEach(list -> list.forEach(c -> ids.add(c.getId())));
        return ids;
    }

    private Set<Long> loadLikedCommentIds(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return Set.of();
        }
        List<CommentLike> likes = commentLikeRepository.findByUserIdAndCommentIdIn(userId, commentIds);
        Set<Long> ids = new HashSet<>();
        for (CommentLike like : likes) {
            ids.add(like.getCommentId());
        }
        return ids;
    }

    private Set<Long> loadFavoritedCommentIds(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return Set.of();
        }
        List<CommentFavorite> favorites = commentFavoriteRepository.findByUserIdAndCommentIdIn(userId, commentIds);
        Set<Long> ids = new HashSet<>();
        for (CommentFavorite favorite : favorites) {
            ids.add(favorite.getCommentId());
        }
        return ids;
    }
}

