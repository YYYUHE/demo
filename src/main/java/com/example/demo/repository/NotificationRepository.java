package com.example.demo.repository;

import com.example.demo.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndTypeOrderByCreateTimeDesc(Long userId, String type, Pageable pageable);

    void deleteByUserIdAndId(Long userId, Long id);
}
