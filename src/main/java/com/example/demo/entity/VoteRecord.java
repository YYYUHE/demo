package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 投票记录实体类（用于防刷和审计）
 */
@Entity
@Table(name = "vote_records", indexes = {
    @Index(name = "idx_vote_user", columnList = "voteId, userId"),
    @Index(name = "idx_vote_ip", columnList = "voteId, ip")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long voteId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String ip;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
}
