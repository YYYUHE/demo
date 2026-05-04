package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 投票实体类
 */
@Entity
@Table(name = "votes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关联的帖子ID
    @Column(nullable = false)
    private Long postId;

    // 投票标题
    @Column(nullable = false, length = 200)
    private String title;

    // 截止时间（可选）
    @Column(nullable = true)
    private LocalDateTime deadline;

    // 最多可选几项
    @Column(nullable = false)
    private Integer maxChoices;

    // 总投票人数
    @Column(nullable = false)
    private Integer totalVoters;

    // 是否已结束
    @Column(nullable = false)
    private Boolean isEnded;

    // 投票选项列表
    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<VoteOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;


    /**
     * 添加投票选项
     */
    public void addOption(VoteOption option) {
        options.add(option);
        option.setVote(this);
    }

    /**
     * 移除投票选项
     */
    public void removeOption(VoteOption option) {
        options.remove(option);
        option.setVote(null);
    }

    /**
     * 检查投票是否已过期
     */
    public boolean checkIfExpired() {
        if (deadline == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(deadline);
    }
}
