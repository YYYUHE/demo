package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentionDto {
    
    private Long id;
    private Long postId;
    private String postTitle;
    private Long commentId;
    private String contentPreview;
    private Long mentionerId;
    private String mentionerUsername;
    private String mentionerAvatar;
    private LocalDateTime createTime;
    private Boolean isRead;
}
