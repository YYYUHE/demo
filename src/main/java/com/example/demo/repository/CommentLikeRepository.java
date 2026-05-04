package com.example.demo.repository;

import com.example.demo.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    List<CommentLike> findByUserIdAndCommentIdIn(Long userId, List<Long> commentIds);
}

