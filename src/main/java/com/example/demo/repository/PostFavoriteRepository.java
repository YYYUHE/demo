package com.example.demo.repository;

import com.example.demo.entity.PostFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {
    Optional<PostFavorite> findByPostIdAndUserId(Long postId, Long userId);
    List<PostFavorite> findByUserIdAndPostIdIn(Long userId, Collection<Long> postIds);
    
    /**
     * 分页查询用户的收藏记录
     */
    @Query("SELECT pf FROM PostFavorite pf WHERE pf.userId = :userId ORDER BY pf.createTime DESC")
    Page<PostFavorite> findByUserIdOrderByCreateTimeDesc(@Param("userId") Long userId, Pageable pageable);
}
