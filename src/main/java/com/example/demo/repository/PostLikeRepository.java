package com.example.demo.repository;

import com.example.demo.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    /**
     * 根据帖子ID和用户ID查找点赞记录
     */
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);
    
    /**
     * 统计帖子的点赞数
     */
    long countByPostId(Long postId);
    
    /**
     * 批量查询用户的点赞记录
     */
    List<PostLike> findByUserIdAndPostIdIn(Long userId, Collection<Long> postIds);
    
    /**
     * 删除点赞记录
     */
    void deleteByPostIdAndUserId(Long postId, Long userId);
}
