package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment_favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"commentId", "userId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long commentId;

    @Column(nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
}
