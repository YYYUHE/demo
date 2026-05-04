package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    // 封面图片URL（第一张图片）
    @Column(length = 500)
    private String coverImage;

    // 帖子分类
    @Column(length = 20)
    private String category;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isAnonymous = Boolean.FALSE;

    @Column(length = 20)
    private String postType;

    // 作者ID
    @Column(nullable = true)
    private Long authorId;

    // 作者用户名（冗余字段，方便查询）
    @Column(length = 50)
    private String authorUsername;

    // 作者头像（冗余字段，方便查询）
    @Column(columnDefinition = "TEXT")
    private String authorAvatar;

    // 图片列表（最多3张，非持久化字段）
    @Transient
    private List<String> images;

    // 文本内容预览（前10个字，非持久化字段）
    @Transient
    private String textPreview;

    // 关联的投票ID（可选）
    @Column(nullable = true)
    private Long voteId;

    // 投票信息（非持久化字段，用于前端展示）
    @Transient
    private Vote vote;

    // 关联的话题列表（多对多关系）
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "post_topics",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Set<Topic> topics = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
