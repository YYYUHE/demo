package com.example.demo.service;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * 用户注册
     */
    @Transactional
    public User register(UserDto userDto) {
        // 验证用户名格式
        if (userDto.getUsername() == null || userDto.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        
        if (userDto.getUsername().length() < 3 || userDto.getUsername().length() > 50) {
            throw new RuntimeException("用户名长度必须在3-50个字符之间");
        }
        
        // 验证密码
        if (userDto.getPassword() == null || userDto.getPassword().length() < 6) {
            throw new RuntimeException("密码长度不能少于6个字符");
        }
        
        // 生成基于时间的UID
        String uid = generateUid();
        
        // 创建新用户
        User user = User.builder()
                .uid(uid)
                .username(userDto.getUsername().trim())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .build();
        
        return userRepository.save(user);
    }
    
    /**
     * 用户登录
     */
    public User login(UserDto userDto) {
        // 查找用户（使用UID）
        User user = userRepository.findByUid(userDto.getUid())
                .orElseThrow(() -> new RuntimeException("UID或密码错误"));
        
        // 验证密码
        if (!passwordEncoder.matches(userDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("UID或密码错误");
        }
        
        return user;
    }
    
    /**
     * 根据ID获取用户
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    /**
     * 生成基于时间的UID
     * 格式：U + 时间戳（毫秒）+ 随机数（4位）
     */
    private String generateUid() {
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 10000);
        return String.format("U%d%04d", timestamp, random);
    }
}
