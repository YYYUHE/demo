package com.example.demo.service;

import com.example.demo.dto.MentionDto;
import com.example.demo.entity.Mention;
import com.example.demo.entity.Post;
import com.example.demo.entity.User;
import com.example.demo.repository.MentionRepository;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MentionService {
    
    private final MentionRepository mentionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    // 匹配 @[uid:xxx] 格式
    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\[uid:(\\d+)\\]");
    
    /**
     * 从内容中提取被@的用户ID列表
     */
    public List<Long> extractMentionedUserIds(String content) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }
        
        List<Long> userIds = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            Long userId = Long.parseLong(matcher.group(1));
            if (!userIds.contains(userId)) {
                userIds.add(userId);
            }
        }
        return userIds;
    }
    
    /**
     * 创建@提及记录并发送通知
     */
    @Transactional
    public void createMentions(Long postId, Long commentId, String content, Long mentionerId) {
        List<Long> mentionedUserIds = extractMentionedUserIds(content);
        
        for (Long mentionedUserId : mentionedUserIds) {
            // 不能@自己
            if (mentionedUserId.equals(mentionerId)) {
                continue;
            }
            
            // 验证被@的用户是否存在
            if (!userRepository.existsById(mentionedUserId)) {
                continue;
            }
            
            // 提取内容预览（前200字符）
            String preview = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            
            // 创建提及记录
            Mention mention = Mention.builder()
                    .postId(postId)
                    .commentId(commentId)
                    .mentionedUserId(mentionedUserId)
                    .mentionerId(mentionerId)
                    .contentPreview(preview)
                    .isRead(false)
                    .build();
            
            mentionRepository.save(mention);
        }
    }
    
    /**
     * 获取用户的@我的列表
     */
    public Page<MentionDto> getMyMentions(Long userId, Pageable pageable) {
        Page<Mention> mentions = mentionRepository.findByMentionedUserIdOrderByCreateTimeDesc(userId, pageable);
        
        return mentions.map(mention -> {
            Post post = postRepository.findById(mention.getPostId()).orElse(null);
            User mentioner = userRepository.findById(mention.getMentionerId()).orElse(null);
            
            return MentionDto.builder()
                    .id(mention.getId())
                    .postId(mention.getPostId())
                    .postTitle(post != null ? post.getTitle() : "未知帖子")
                    .commentId(mention.getCommentId())
                    .contentPreview(mention.getContentPreview())
                    .mentionerId(mention.getMentionerId())
                    .mentionerUsername(mentioner != null ? mentioner.getUsername() : "未知用户")
                    .mentionerAvatar(mentioner != null ? mentioner.getAvatar() : "")
                    .createTime(mention.getCreateTime())
                    .isRead(mention.getIsRead())
                    .build();
        });
    }
    
    /**
     * 标记@通知为已读
     */
    @Transactional
    public void markAsRead(Long mentionId, Long userId) {
        Mention mention = mentionRepository.findById(mentionId)
                .orElseThrow(() -> new RuntimeException("提及记录不存在"));
        
        if (!mention.getMentionedUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        
        mention.setIsRead(true);
        mentionRepository.save(mention);
    }
    
    /**
     * 删除@通知
     */
    @Transactional
    public void deleteMention(Long mentionId, Long userId) {
        Mention mention = mentionRepository.findById(mentionId)
                .orElseThrow(() -> new RuntimeException("提及记录不存在"));
        
        if (!mention.getMentionedUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        
        mentionRepository.delete(mention);
    }
    
    /**
     * 获取未读@数量
     */
    public long getUnreadCount(Long userId) {
        return mentionRepository.countByMentionedUserIdAndIsReadFalse(userId);
    }
}
