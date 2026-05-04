package com.example.demo.service;

import com.example.demo.dto.TopicDto;
import com.example.demo.entity.Topic;
import com.example.demo.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 话题服务类
 */
@Service
@RequiredArgsConstructor
public class TopicService {
    
    private final TopicRepository topicRepository;
    
    /**
     * 创建新话题
     */
    @Transactional
    public TopicDto createTopic(String name, String description) {
        // 验证话题名称
        validateTopicName(name);
        
        // 检查是否已存在
        if (topicRepository.existsByName(name)) {
            throw new RuntimeException("话题 \"" + name + "\" 已存在");
        }
        
        // 创建话题
        Topic topic = Topic.builder()
                .name(name.trim())
                .description(description)
                .usageCount(0)
                .isActive(true)
                .build();
        
        Topic savedTopic = topicRepository.save(topic);
        return convertToDto(savedTopic);
    }
    
    /**
     * 获取或创建话题（如果不存在则创建）
     */
    @Transactional
    public TopicDto getOrCreateTopic(String name) {
        // 尝查找现有话题
        return topicRepository.findByName(name)
                .map(this::convertToDto)
                .orElseGet(() -> createTopic(name, null));
    }
    
    /**
     * 根据ID获取话题
     */
    public TopicDto getTopicById(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("话题不存在"));
        return convertToDto(topic);
    }
    
    /**
     * 根据名称获取话题
     */
    public TopicDto getTopicByName(String name) {
        Topic topic = topicRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("话题不存在: " + name));
        return convertToDto(topic);
    }
    
    /**
     * 搜索话题（模糊匹配）
     */
    public List<TopicDto> searchTopics(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        
        List<Topic> topics = topicRepository.findByNameContainingIgnoreCase(keyword.trim());
        return topics.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取热门话题
     */
    public List<TopicDto> getPopularTopics(int limit) {
        List<Topic> topics = topicRepository.findTopPopularTopics(PageRequest.of(0, limit));
        return topics.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取搜索建议（前缀匹配）
     */
    public List<TopicDto> getSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return List.of();
        }
        
        List<Topic> topics = topicRepository.findSuggestionsByPrefix(prefix.trim(), PageRequest.of(0, limit));
        return topics.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 增加话题使用次数
     */
    @Transactional
    public void incrementUsage(String topicName) {
        topicRepository.findByName(topicName).ifPresent(topic -> {
            topic.incrementUsage();
            topicRepository.save(topic);
        });
    }
    
    /**
     * 减少话题使用次数
     */
    @Transactional
    public void decrementUsage(String topicName) {
        topicRepository.findByName(topicName).ifPresent(topic -> {
            topic.decrementUsage();
            topicRepository.save(topic);
        });
    }
    
    /**
     * 删除话题
     */
    @Transactional
    public void deleteTopic(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("话题不存在"));
        
        // 检查是否有帖子使用该话题
        if (!topic.getPosts().isEmpty()) {
            throw new RuntimeException("该话题已被使用，无法删除");
        }
        
        topicRepository.delete(topic);
    }
    
    /**
     * 验证话题名称
     */
    private void validateTopicName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("话题名称不能为空");
        }
        
        String trimmed = name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 20) {
            throw new RuntimeException("话题名称长度必须在2-20个字符之间");
        }
        
        // 只允许中英文、数字和下划线
        if (!trimmed.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$")) {
            throw new RuntimeException("话题名称只能包含中英文、数字和下划线");
        }
    }
    
    /**
     * 转换为DTO
     */
    private TopicDto convertToDto(Topic topic) {
        return TopicDto.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .usageCount(topic.getUsageCount())
                .isActive(topic.getIsActive())
                .createTime(topic.getCreateTime())
                .updateTime(topic.getUpdateTime())
                .build();
    }
}
