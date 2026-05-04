package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据UID查找用户
     */
    Optional<User> findByUid(String uid);
    
    /**
     * 检查UID是否存在
     */
    boolean existsByUid(String uid);
    
    /**
     * 根据用户名查找用户（用户名不再唯一，返回第一个匹配）
     */
    Optional<User> findByUsername(String username);
}
