package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投票数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteDto {
    
    private Long id;
    private Long postId;
    private String title;
    private LocalDateTime deadline;
    private Integer maxChoices;
    private Integer totalVoters;
    private Boolean isEnded;
    private Boolean hasVoted; // 当前用户是否已投票
    private List<VoteOptionDto> options;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
