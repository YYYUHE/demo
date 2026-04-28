package com.example.demo.repository;

import com.example.demo.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    /**
     * 按创建时间倒序查询所有帖子
     */
    List<Post> findAllByOrderByCreateTimeDesc();
    
    /**
     * 按分类和创建时间倒序查询帖子
     */
    List<Post> findByCategoryOrderByCreateTimeDesc(String category);
    
    /**
     * 按标题模糊搜索（不区分大小写）
     */
    List<Post> findByTitleContainingIgnoreCaseOrderByCreateTimeDesc(String keyword);
    
    /**
     * 按分类和标题模糊搜索
     */
    List<Post> findByCategoryAndTitleContainingIgnoreCaseOrderByCreateTimeDesc(String category, String keyword);
    
    /**
     * 按作者ID和创建时间倒序查询帖子
     */
    List<Post> findByAuthorIdOrderByCreateTimeDesc(Long authorId);
    
    /**
     * 按话题名称搜索帖子（通过JOIN查询）
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT p FROM Post p JOIN p.topics t WHERE t.name = :topicName ORDER BY p.createTime DESC"
    )
    List<Post> findByTopicNameOrderByCreateTimeDesc(@org.springframework.data.repository.query.Param("topicName") String topicName);

    Optional<Post> findTopByAuthorIdOrderByCreateTimeDesc(Long authorId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT p FROM Post p WHERE " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))"
    )
    Page<Post> searchPosts(@org.springframework.data.repository.query.Param("category") String category,
                           @org.springframework.data.repository.query.Param("keyword") String keyword,
                           Pageable pageable);

    Page<Post> findByAuthorIdOrderByCreateTimeDesc(Long authorId, Pageable pageable);
    
    /**
     * 根据ID列表、分类和标题关键词查询帖子
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT p FROM Post p WHERE p.id IN :postIds AND p.category = :category AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.createTime DESC"
    )
    List<Post> findByIdInAndCategoryAndTitleContainingOrderByCreateTimeDesc(
            @org.springframework.data.repository.query.Param("postIds") List<Long> postIds,
            @org.springframework.data.repository.query.Param("category") String category,
            @org.springframework.data.repository.query.Param("keyword") String keyword);
    
    /**
     * 根据ID列表和分类查询帖子
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT p FROM Post p WHERE p.id IN :postIds AND p.category = :category ORDER BY p.createTime DESC"
    )
    List<Post> findByIdInAndCategoryOrderByCreateTimeDesc(
            @org.springframework.data.repository.query.Param("postIds") List<Long> postIds,
            @org.springframework.data.repository.query.Param("category") String category);
    
    /**
     * 根据ID列表和标题关键词查询帖子
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT p FROM Post p WHERE p.id IN :postIds AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.createTime DESC"
    )
    List<Post> findByIdInAndTitleContainingOrderByCreateTimeDesc(
            @org.springframework.data.repository.query.Param("postIds") List<Long> postIds,
            @org.springframework.data.repository.query.Param("keyword") String keyword);
}
