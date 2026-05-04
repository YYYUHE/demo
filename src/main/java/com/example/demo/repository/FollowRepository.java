package com.example.demo.repository;

import com.example.demo.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 关注关系数据访问接口
 */
@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    
    /**
     * 根据关注者ID和被关注者ID查找关注关系
     */
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    /**
     * 查找用户关注的所有人
     */
    @Query("SELECT f.followingId FROM Follow f WHERE f.followerId = :followerId ORDER BY f.createTime DESC")
    List<Long> findFollowingIdsByFollowerId(Long followerId);
    
    /**
     * 查找关注某个用户的所有人
     */
    @Query("SELECT f.followerId FROM Follow f WHERE f.followingId = :followingId ORDER BY f.createTime DESC")
    List<Long> findFollowerIdsByFollowingId(Long followingId);
    
    /**
     * 统计用户关注的人数
     */
    long countByFollowerId(Long followerId);
    
    /**
     * 统计用户的粉丝数
     */
    long countByFollowingId(Long followingId);
    
    /**
     * 删除关注关系
     */
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
}
