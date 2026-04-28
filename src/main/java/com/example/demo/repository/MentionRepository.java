package com.example.demo.repository;

import com.example.demo.entity.Mention;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentionRepository extends JpaRepository<Mention, Long> {
    
    Page<Mention> findByMentionedUserIdOrderByCreateTimeDesc(Long mentionedUserId, Pageable pageable);
    
    List<Mention> findByPostIdAndCommentIdIsNull(Long postId);
    
    List<Mention> findByCommentId(Long commentId);
    
    long countByMentionedUserIdAndIsReadFalse(Long mentionedUserId);
}
