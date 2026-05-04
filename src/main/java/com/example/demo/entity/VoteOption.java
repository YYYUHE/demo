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
import java.util.Set;

/**
 * 投票选项实体类
 */
@Entity
@Table(name = "vote_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteOption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联的投票ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    // 选项内容
    @Column(nullable = false, length = 500)
    private String content;

    // 排序顺序
    @Column(nullable = false)
    private Integer sortOrder;

    // 得票数
    @Column(nullable = false)
    private Integer voteCount;

    // 投票用户ID集合（用于防止重复投票）
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vote_option_users", joinColumns = @JoinColumn(name = "option_id"))
    @Column(name = "user_id")
    private Set<Long> voterIds = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;

    /**
     * 添加投票用户
     */
    public void addVoter(Long userId) {
        if (voterIds == null) {
            voterIds = new HashSet<>();
        }
        voterIds.add(userId);
        this.voteCount++;
    }

    /**
     * 移除投票用户（取消投票）
     */
    public void removeVoter(Long userId) {
        if (voterIds != null && voterIds.contains(userId)) {
            voterIds.remove(userId);
            this.voteCount--;
        }
    }

    /**
     * 检查用户是否已投票
     */
    public boolean hasVoted(Long userId) {
        return voterIds != null && voterIds.contains(userId);
    }
}
