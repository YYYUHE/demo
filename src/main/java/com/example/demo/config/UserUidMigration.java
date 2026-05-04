package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据迁移：为现有用户补充UID
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserUidMigration implements CommandLineRunner {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public void run(String... args) {
        log.info("开始检查用户UID迁移...");
        
        // 查找所有uid为null的用户
        List<User> usersWithoutUid = userRepository.findAll().stream()
                .filter(user -> user.getUid() == null || user.getUid().isEmpty())
                .toList();
        
        if (usersWithoutUid.isEmpty()) {
            log.info("所有用户已有UID，无需迁移");
            return;
        }
        
        log.info("发现 {} 个用户需要补充UID", usersWithoutUid.size());
        
        int count = 0;
        for (User user : usersWithoutUid) {
            // 生成UID
            String uid = generateUid();
            user.setUid(uid);
            userRepository.save(user);
            count++;
            log.info("为用户 [{}] 生成UID: {}", user.getUsername(), uid);
        }
        
        log.info("UID迁移完成，共更新 {} 个用户", count);
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
