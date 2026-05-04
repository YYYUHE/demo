package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 用户唯一标识UID（基于时间生成）
    @Column(nullable = false, unique = true, length = 50)
    private String uid;

    // 用户名（不再唯一，仅用于显示）
    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 200)
    private String password;

    // 头像URL（Base64编码）
    @Column(columnDefinition = "TEXT")
    private String avatar;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
}
