package com.example.demo.repository;

import com.example.demo.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdAndDepth(Long postId, Integer depth, Pageable pageable);

    List<Comment> findByRootIdAndDepth(Long rootId, Integer depth, Pageable pageable);

    List<Comment> findByParentId(Long parentId, Pageable pageable);

    long countByRootIdAndDepth(Long rootId, Integer depth);

    long countByParentId(Long parentId);

    Optional<Comment> findByIdAndPostId(Long id, Long postId);

    // 查询当前用户发表的所有评论
    List<Comment> findByAuthorId(Long authorId);

    // 查询所有回复指定父评论ID列表的回复（depth >= 2）
    Page<Comment> findByParentIdInAndDepthGreaterThanOrderByCreateTimeDesc(List<Long> parentIds, Integer depth, Pageable pageable);

    // 根据父评论ID和帖子ID查找父评论
    Optional<Comment> findByIdAndPostIdAndDepth(Long id, Long postId, Integer depth);

    // 查询指定帖子列表中的一级评论（depth=1），排除指定作者
    Page<Comment> findByPostIdInAndDepthAndAuthorIdNotOrderByCreateTimeDesc(
            java.util.List<Long> postIds, Integer depth, Long authorId, Pageable pageable);
}

