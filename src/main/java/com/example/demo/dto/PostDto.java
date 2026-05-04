package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {
    private Long id;
    private String title;
    private String content;
    private String coverImage;
    private List<String> images; // 图片列表（最多3张）
    private String textPreview; // 文本内容预览（前10个字）
    private String category;
    private Boolean isAnonymous;
    private String postType;
    
    // 作者信息
    private Long authorId;
    private String authorUsername;
    private String authorAvatar;
    
    // 投票信息
    private Long voteId;
    private com.example.demo.dto.VoteDto vote;
    
    // 话题列表（话题名称）
    private Set<String> topicNames;
}
