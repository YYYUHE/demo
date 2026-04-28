package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户关注关系实体
 */
@Entity
@Table(
    name = "user_follows",
    uniqueConstraints = @UniqueConstraint(columnNames = {"followerId", "followingId"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关注者ID
    @Column(nullable = false)
    private Long followerId;

    // 被关注者ID
    @Column(nullable = false)
    private Long followingId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
}
