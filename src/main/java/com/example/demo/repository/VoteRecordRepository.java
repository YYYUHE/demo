package com.example.demo.repository;

import com.example.demo.entity.VoteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {
    
    /**
     * 根据投票ID和用户ID查找记录
     */
    Optional<VoteRecord> findByVoteIdAndUserId(Long voteId, Long userId);
    
    /**
     * 根据投票ID和IP查找记录（用于防刷）
     */
    Optional<VoteRecord> findByVoteIdAndIp(Long voteId, String ip);
}
