package com.example.demo.repository;

import com.example.demo.entity.Draft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DraftRepository extends JpaRepository<Draft, Long> {
    
    /**
     * 按更新时间倒序查询所有草稿
     */
    List<Draft> findByUserIdOrderByUpdateTimeDesc(Long userId);
    Optional<Draft> findByIdAndUserId(Long id, Long userId);
    boolean existsByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
    void deleteByIdInAndUserId(Collection<Long> ids, Long userId);
}
