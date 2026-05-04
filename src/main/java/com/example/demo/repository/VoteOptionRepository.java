package com.example.demo.repository;

import com.example.demo.entity.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {
    
    /**
     * 根据投票ID查找所有选项
     */
    List<VoteOption> findByVoteIdOrderBySortOrderAsc(Long voteId);
    
    /**
     * 删除指定投票的所有选项
     */
    void deleteByVoteId(Long voteId);
}
