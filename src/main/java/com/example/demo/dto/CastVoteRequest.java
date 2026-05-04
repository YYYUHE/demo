package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 投票请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CastVoteRequest {
    
    private Long voteId;
    private List<Long> optionIds; // 用户选择的选项ID列表
}
