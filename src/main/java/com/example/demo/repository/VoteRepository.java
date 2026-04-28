package com.example.demo.repository;

import com.example.demo.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {
    
    /**
     * 根据帖子ID查找投票
     */
    Optional<Vote> findByPostId(Long postId);
    
    /**
     * 根据帖子ID列表批量查找投票
     */
    List<Vote> findByPostIdIn(List<Long> postIds);
}
