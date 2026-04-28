package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投票选项数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteOptionDto {
    
    private Long id;
    private Long voteId;
    private String content;
    private Integer sortOrder;
    private Integer voteCount;
    private Boolean voted; // 当前用户是否已投此项
    private Double percentage; // 占比
}
