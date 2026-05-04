package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {

    private Long id;
    private Long postId;
    private Long parentId;
    private Long rootId;
    private Integer depth;
    private String content;
    private Long likeCount;
    private Boolean liked;
    private Boolean favorited;
    private Long authorId;
    private String authorUsername;
    private String authorAvatar;
    private LocalDateTime createTime;

    private Long replyCount;
    private List<CommentDto> replies;
}

