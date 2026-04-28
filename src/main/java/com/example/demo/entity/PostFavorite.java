package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_favorites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"postId", "userId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
}
