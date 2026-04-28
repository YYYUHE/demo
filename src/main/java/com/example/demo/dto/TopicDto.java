package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 话题DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicDto {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private Integer usageCount;
    
    private Boolean isActive;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
