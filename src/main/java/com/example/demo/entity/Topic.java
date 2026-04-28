package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 话题实体类
 */
@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "posts")
public class Topic {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    @EqualsAndHashCode.Include
    private String name;
    
    @Column(length = 200)
    private String description;
    
    @Column(nullable = false)
    private Integer usageCount; // 使用次数（热度）
    
    @Column(nullable = false)
    private Boolean isActive; // 是否激活
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
    
    @ManyToMany(mappedBy = "topics")
    private Set<Post> posts = new HashSet<>();
    
    /**
     * 增加使用次数
     */
    public void incrementUsage() {
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
        this.usageCount++;
    }
    
    /**
     * 减少使用次数
     */
    public void decrementUsage() {
        if (this.usageCount != null && this.usageCount > 0) {
            this.usageCount--;
        }
    }
}
