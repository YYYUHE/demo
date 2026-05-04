package com.example.demo.repository;

import com.example.demo.entity.CommentFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentFavoriteRepository extends JpaRepository<CommentFavorite, Long> {
    Optional<CommentFavorite> findByCommentIdAndUserId(Long commentId, Long userId);
    List<CommentFavorite> findByUserIdAndCommentIdIn(Long userId, Collection<Long> commentIds);
}
