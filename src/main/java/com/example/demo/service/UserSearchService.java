package com.example.demo.service;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.Follow;
import com.example.demo.entity.User;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserSearchService {
    
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    
    /**
     * 搜索当前用户关注的用户
     */
    public List<UserDto> searchFollowingUsers(Long currentUserId, String keyword) {
        // 获取当前用户关注的所有用户ID
        List<Long> followingIds = followRepository
                .findFollowingIdsByFollowerId(currentUserId);
        
        if (followingIds.isEmpty()) {
            return List.of();
        }
        
        // 在关注的用户中搜索
        List<User> users = userRepository.findAllById(followingIds);
        
        return users.stream()
                .filter(user -> keyword == null || keyword.isEmpty() || 
                        user.getUsername().toLowerCase().contains(keyword.toLowerCase()))
                .limit(10) // 最多返回10个结果
                .map(user -> UserDto.builder()
                        .id(user.getId())
                        .uid(user.getUid())
                        .username(user.getUsername())
                        .avatar(user.getAvatar())
                        .build())
                .collect(Collectors.toList());
    }
}
