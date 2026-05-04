package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 帖子详情DTO（包含话题信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDetailDto {
    private Long id;
    private String title;
    private String content;
    private String coverImage;
    private List<String> images;
    private String textPreview;
    private String category;
    private Boolean isAnonymous;
    private Boolean favorited;
    private Boolean liked;
    private Long likeCount;
    private String postType;
    private Boolean canCancelAnonymous;
    
    // 作者信息
    private Long authorId;
    private String authorUsername;
    private String authorAvatar;
    
    // 投票信息
    private Long voteId;
    private VoteDto vote;
    
    // 话题列表（话题名称）
    @Builder.Default
    private Set<String> topicNames = new HashSet<>();
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
