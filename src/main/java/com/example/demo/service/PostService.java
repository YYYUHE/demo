package com.example.demo.service;

import com.example.demo.dto.PostDetailDto;
import com.example.demo.dto.PostDto;
import com.example.demo.dto.VoteDto;
import com.example.demo.entity.Post;
import com.example.demo.entity.PostFavorite;
import com.example.demo.entity.PostLike;
import com.example.demo.entity.Topic;
import com.example.demo.entity.User;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.PostFavoriteRepository;
import com.example.demo.repository.PostLikeRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.TopicRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final VoteService voteService;
    private final TopicRepository topicRepository;
    private final PostFavoriteRepository postFavoriteRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostRateLimiter postRateLimiter;
    private final NotificationService notificationService;
    private final MentionService mentionService;

    @Transactional
    public Post publishPost(PostDto postDto) {
        postRateLimiter.validateCanPublish(postDto.getAuthorId());
        
        // 交友板块特殊验证：禁止匿名和投票
        if ("交友".equals(postDto.getCategory())) {
            if (Boolean.TRUE.equals(postDto.getIsAnonymous())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 400, "交友分区不支持匿名发帖");
            }
            if (postDto.getVote() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 400, "交友分区不支持投票功能");
            }
        }
        
        if (Boolean.TRUE.equals(postDto.getIsAnonymous()) && "二手".equals(postDto.getCategory())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 400, "二手分区不支持匿名发帖");
        }
        String coverImage = extractFirstImage(postDto.getContent());
        Post post = Post.builder()
                .title(postDto.getTitle())
                .content(postDto.getContent())
                .coverImage(coverImage)
                .category(postDto.getCategory())
                .isAnonymous(Boolean.TRUE.equals(postDto.getIsAnonymous()))
                .postType(normalizePostType(postDto.getPostType()))
                .authorId(postDto.getAuthorId())
                .authorUsername(postDto.getAuthorUsername())
                .authorAvatar(postDto.getAuthorAvatar())
                .build();
        if (postDto.getTopicNames() != null && !postDto.getTopicNames().isEmpty()) {
            post.setTopics(processTopics(postDto.getTopicNames()));
        }
        Post savedPost = postRepository.save(post);
        
        // 处理@提及
        if (postDto.getContent() != null && !postDto.getContent().isEmpty()) {
            mentionService.createMentions(
                    savedPost.getId(),
                    null,  // 帖子中没有commentId
                    postDto.getContent(),
                    postDto.getAuthorId()
            );
        }
        
        return savedPost;
    }

    /**
     * 处理话题列表：获取或创建话题，并更新使用次数
     */
    private Set<Topic> processTopics(Set<String> topicNames) {
        Set<Topic> topics = new HashSet<>();
        for (String topicName : topicNames) {
            if (topicName == null || topicName.trim().isEmpty()) continue;
            try {
                String trimmedName = topicName.trim();
                Topic topic = topicRepository.findByName(trimmedName)
                        .orElseGet(() -> createNewTopic(trimmedName));
                topics.add(topic);
            } catch (Exception e) {
                System.err.println("处理话题失败: " + topicName + ", 错误: " + e.getMessage());
            }
        }
        
        for (Topic topic : topics) {
            try {
                topic.incrementUsage();
                topicRepository.save(topic);
            } catch (Exception e) {
                System.err.println("更新话题使用次数失败: " + topic.getName() + ", 错误: " + e.getMessage());
            }
        }
        
        return topics;
    }

    private Topic createNewTopic(String name) {
        return topicRepository.save(Topic.builder().name(name).description(null).usageCount(0).isActive(true).build());
    }

    public Page<PostDetailDto> listPosts(String category, String search, Pageable pageable, User viewer) {
        String cat = category == null || category.trim().isEmpty() ? null : category.trim();
        String keyword = search == null || search.trim().isEmpty() ? null : search.trim();
        if (keyword != null && keyword.startsWith("#")) {
            List<Post> posts = postRepository.findByTopicNameOrderByCreateTimeDesc(keyword.substring(1).trim());
            return toPage(posts, pageable, viewer);
        }
        Page<Post> postPage = postRepository.searchPosts(cat, keyword, pageable);
        return toDtoPage(postPage, viewer);
    }

    public Page<PostDetailDto> listMyPosts(Long authorId, Pageable pageable, User viewer) {
        return toDtoPage(postRepository.findByAuthorIdOrderByCreateTimeDesc(authorId, pageable), viewer);
    }

    /**
     * 获取用户的收藏列表
     */
    public Page<PostDetailDto> listFavoritePosts(Long userId, String category, String keyword, Pageable pageable, User viewer) {
        // 先查询用户的收藏记录
        Page<PostFavorite> favoritePage = postFavoriteRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
        
        if (favoritePage.isEmpty()) {
            return Page.empty(pageable);
        }
        
        // 获取收藏的帖子ID列表
        List<Long> postIds = favoritePage.getContent().stream()
                .map(PostFavorite::getPostId)
                .collect(Collectors.toList());
        
        // 根据帖子ID查询帖子详情，支持分类和关键词筛选
        List<Post> posts;
        if (category != null && !category.trim().isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            // 同时有分类和关键词
            posts = postRepository.findByIdInAndCategoryAndTitleContainingOrderByCreateTimeDesc(
                    postIds, category.trim(), keyword.trim());
        } else if (category != null && !category.trim().isEmpty()) {
            // 只有分类
            posts = postRepository.findByIdInAndCategoryOrderByCreateTimeDesc(postIds, category.trim());
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            // 只有关键词
            posts = postRepository.findByIdInAndTitleContainingOrderByCreateTimeDesc(postIds, keyword.trim());
        } else {
            // 无筛选条件
            posts = postRepository.findAllById(postIds);
            // 按照收藏顺序排序
            posts.sort((p1, p2) -> {
                int idx1 = postIds.indexOf(p1.getId());
                int idx2 = postIds.indexOf(p2.getId());
                return Integer.compare(idx1, idx2);
            });
        }
        
        // 转换为分页结果
        return toDtoPage(new PageImpl<>(posts, pageable, favoritePage.getTotalElements()), viewer);
    }

    private Page<PostDetailDto> toPage(List<Post> all, Pageable pageable, User viewer) {
        int from = Math.min((int) pageable.getOffset(), all.size());
        int to = Math.min(from + pageable.getPageSize(), all.size());
        return toDtoPage(new PageImpl<>(all.subList(from, to), pageable, all.size()), viewer);
    }

    private Page<PostDetailDto> toDtoPage(Page<Post> postPage, User viewer) {
        List<Post> posts = postPage.getContent();
        fillAuthorInfo(posts);
        Set<Long> favoritedIds = loadFavoritedIds(viewer == null ? null : viewer.getId(), posts.stream().map(Post::getId).toList());
        Set<Long> likedIds = loadLikedIds(viewer == null ? null : viewer.getId(), posts.stream().map(Post::getId).toList());
        Map<Long, Long> likeCounts = loadLikeCounts(posts.stream().map(Post::getId).toList());
        List<PostDetailDto> dtos = posts.stream().map(p -> {
            boolean favorited = favoritedIds.contains(p.getId());
            boolean liked = likedIds.contains(p.getId());
            long likeCount = likeCounts.getOrDefault(p.getId(), 0L);
            return toDto(p, viewer, favorited, liked, likeCount);
        }).toList();
        return new PageImpl<>(dtos, postPage.getPageable(), postPage.getTotalElements());
    }

    @Transactional
    public PostDetailDto getPostDetailById(Long id, User viewer) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 404, "帖子不存在"));
        fillAuthorInfo(List.of(post));
        boolean favorited = viewer != null && postFavoriteRepository.findByPostIdAndUserId(id, viewer.getId()).isPresent();
        boolean liked = viewer != null && postLikeRepository.findByPostIdAndUserId(id, viewer.getId()).isPresent();
        long likeCount = postLikeRepository.countByPostId(id);
        PostDetailDto dto = toDto(post, viewer, favorited, liked, likeCount);
        if (post.getVoteId() != null) {
            try {
                System.out.println("📊 加载投票数据，帖子ID: " + id + ", 投票ID: " + post.getVoteId());
                VoteDto voteDto = voteService.getVoteById(post.getVoteId(), viewer);
                dto.setVote(voteDto);
                System.out.println("✅ 投票数据加载成功，标题: " + voteDto.getTitle());
            } catch (Exception e) {
                System.err.println("❌ 加载投票数据失败，帖子ID: " + id + ", 投票ID: " + post.getVoteId() + ", 错误: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️ 帖子没有关联投票，帖子ID: " + id);
        }
        return dto;
    }

    public PostDetailDto getPostDetailById(Long id) {
        return getPostDetailById(id, null);
    }

    /**
     * 根据ID获取帖子实体（用于投票创建后重新加载）
     */
    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 404, "帖子不存在"));
    }

    private void fillAuthorInfo(List<Post> posts) {
        posts.forEach(post -> {
            if (post.getAuthorId() != null) {
                userRepository.findById(post.getAuthorId()).ifPresent(user -> {
                    post.setAuthorUsername(user.getUsername());
                    post.setAuthorAvatar(user.getAvatar());
                });
            }
        });
    }

    private Set<Long> loadFavoritedIds(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) return Set.of();
        return postFavoriteRepository.findByUserIdAndPostIdIn(userId, postIds).stream()
                .map(PostFavorite::getPostId)
                .collect(Collectors.toSet());
    }

    private Set<Long> loadLikedIds(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) return Set.of();
        return postLikeRepository.findByUserIdAndPostIdIn(userId, postIds).stream()
                .map(PostLike::getPostId)
                .collect(Collectors.toSet());
    }

    private Map<Long, Long> loadLikeCounts(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Map.of();
        Map<Long, Long> likeCounts = new HashMap<>();
        for (Long postId : postIds) {
            likeCounts.put(postId, postLikeRepository.countByPostId(postId));
        }
        return likeCounts;
    }

    private PostDetailDto toDto(Post post, User viewer, boolean favorited, boolean liked, long likeCount) {
        Set<String> topicNames = post.getTopics() == null ? new HashSet<>() :
                post.getTopics().stream().map(Topic::getName).collect(Collectors.toSet());
        boolean reveal = !Boolean.TRUE.equals(post.getIsAnonymous()) ||
                (viewer != null && (Objects.equals(viewer.getId(), post.getAuthorId()) || "admin".equalsIgnoreCase(viewer.getUsername())));
        boolean canCancelAnonymous = Boolean.TRUE.equals(post.getIsAnonymous())
                && viewer != null
                && Objects.equals(viewer.getId(), post.getAuthorId());
        PostDetailDto dto = PostDetailDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .coverImage(post.getCoverImage())
                .category(post.getCategory())
                .isAnonymous(Boolean.TRUE.equals(post.getIsAnonymous()))
                .favorited(favorited)
                .liked(liked)
                .likeCount(likeCount)
                .postType(post.getPostType())
                .authorId(reveal ? post.getAuthorId() : null)
                .authorUsername(reveal ? post.getAuthorUsername() : "匿名用户")
                .authorAvatar(reveal ? post.getAuthorAvatar() : "")
                .canCancelAnonymous(canCancelAnonymous)
                .voteId(post.getVoteId())
                .createTime(post.getCreateTime())
                .updateTime(post.getUpdateTime())
                .images(extractImages(post.getContent(), 0))
                .textPreview(extractTextPreview(post.getContent()))
                .topicNames(topicNames)
                .build();
        return dto;
    }

    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 404, "帖子不存在"));
        if (post.getAuthorId() == null || !post.getAuthorId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, 403, "您没有权限删除此帖子");
        }
        postRepository.deleteById(id);
    }

    @Transactional
    public void cancelAnonymous(Long id, Long userId) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 404, "帖子不存在"));
        if (!Objects.equals(post.getAuthorId(), userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, 403, "仅作者可取消匿名");
        }
        post.setIsAnonymous(false);
        postRepository.save(post);
    }

    @Transactional
    public void favoritePost(Long postId, Long userId) {
        postRepository.findById(postId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 404, "帖子不存在"));
        postFavoriteRepository.findByPostIdAndUserId(postId, userId).orElseGet(() ->
                postFavoriteRepository.save(PostFavorite.builder().postId(postId).userId(userId).build()));
    }

    @Transactional
    public void unfavoritePost(Long postId, Long userId) {
        postFavoriteRepository.findByPostIdAndUserId(postId, userId).ifPresent(postFavoriteRepository::delete);
    }

    /**
     * 切换点赞状态（点赞/取消点赞）
     */
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 404, "帖子不存在"));
        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
        if (existingLike.isPresent()) {
            // 已点赞，取消点赞
            postLikeRepository.delete(existingLike.get());
            return false; // 返回false表示未点赞
        } else {
            // 未点赞，添加点赞
            postLikeRepository.save(PostLike.builder().postId(postId).userId(userId).build());
            
            // 创建点赞通知（如果不是自己给自己点赞）
            if (!post.getAuthorId().equals(userId)) {
                userRepository.findById(userId).ifPresent(user -> {
                    notificationService.createLikeNotification(
                            post.getAuthorId(),
                            postId,
                            null,
                            null,
                            "post",
                            user.getId(),
                            user.getUsername(),
                            user.getAvatar(),
                            post.getTitle(),
                            post.getTitle()
                    );
                });
            }
            
            return true; // 返回true表示已点赞
        }
    }

    /**
     * 获取帖子的点赞数
     */
    public long getLikeCount(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }

    private String extractFirstImage(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(htmlContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<String> extractImages(String htmlContent, int limit) {
        List<String> images = new ArrayList<>();
        if (htmlContent == null || htmlContent.isEmpty()) return images;
        Pattern pattern = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(htmlContent);
        int count = 0;
        while (matcher.find() && (limit <= 0 || count < limit)) {
            images.add(matcher.group(1));
            count++;
        }
        return images;
    }

    private String extractTextPreview(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) return "";
        try {
            String plainText = htmlContent.replaceAll("<[^>]*>", "");
            plainText = plainText.replaceAll("</?\\w+[^a-zA-Z0-9][^<]*", "");
            plainText = plainText.replaceAll("&nbsp;", " ")
                    .replaceAll("&amp;", "&")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">")
                    .replaceAll("&quot;", "\"")
                    .replaceAll("&#39;", "'");
            String[] lines = plainText.split("\n");
            StringBuilder textBuilder = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("#")) {
                    String withoutTags = line.replaceAll("#[^\\s#]+", "").trim();
                    if (withoutTags.isEmpty()) continue;
                }
                if (!line.isEmpty()) {
                    if (textBuilder.length() > 0) textBuilder.append(" ");
                    textBuilder.append(line);
                }
            }
            String text = textBuilder.toString().replaceAll("\\s+", " ").trim();
            if (text.length() > 10) text = text.substring(0, 10);
            text = text.replaceAll("</?\\w+[^a-zA-Z0-9][^<]*", "").trim();
            return text;
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizePostType(String postType) {
        String t = postType == null ? "" : postType.trim().toLowerCase();
        if ("image".equals(t)) return "image";
        return "text";
    }
}
