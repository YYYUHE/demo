package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建投票请求对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoteRequest {
    
    private String title;
    private LocalDateTime deadline;
    private Integer maxChoices;
    private List<String> options; // 选项内容列表
}
