package com.example.demo.repository;

import com.example.demo.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 话题数据访问接口
 */
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    
    /**
     * 根据名称查找话题
     */
    Optional<Topic> findByName(String name);
    
    /**
     * 检查话题是否存在
     */
    boolean existsByName(String name);
    
    /**
     * 模糊搜索话题（按名称）
     */
    List<Topic> findByNameContainingIgnoreCase(String keyword);
    
    /**
     * 获取热门话题（按使用次数排序）
     */
    @Query("SELECT t FROM Topic t WHERE t.isActive = true ORDER BY t.usageCount DESC")
    List<Topic> findPopularTopics();
    
    /**
     * 获取热门话题（限制数量）
     */
    @Query("SELECT t FROM Topic t WHERE t.isActive = true ORDER BY t.usageCount DESC")
    List<Topic> findTopPopularTopics(org.springframework.data.domain.Pageable pageable);
    
    /**
     * 搜索建议（前缀匹配，限制数量）
     */
    @Query("SELECT t FROM Topic t WHERE t.isActive = true AND LOWER(t.name) LIKE LOWER(CONCAT(:prefix, '%')) ORDER BY t.usageCount DESC")
    List<Topic> findSuggestionsByPrefix(String prefix, org.springframework.data.domain.Pageable pageable);
}
