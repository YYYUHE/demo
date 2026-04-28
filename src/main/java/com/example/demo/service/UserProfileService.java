package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    
    private final UserRepository userRepository;
    
    /**
     * 更新用户头像
     */
    @Transactional
    public User updateAvatar(Long userId, String avatarBase64) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 验证头像数据
        if (avatarBase64 != null && !avatarBase64.trim().isEmpty()) {
            // 限制头像大小（最大5MB）
            if (avatarBase64.length() > 5 * 1024 * 1024) {
                throw new RuntimeException("头像图片过大，请选择小于5MB的图片");
            }
            user.setAvatar(avatarBase64);
        } else {
            user.setAvatar(null);
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 获取用户完整信息（包含头像）
     */
    public User getUserProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
}
