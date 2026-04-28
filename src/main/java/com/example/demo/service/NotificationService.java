package com.example.demo.service;

import com.example.demo.entity.Notification;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 创建回复通知
     */
    @Transactional
    public void createReplyNotification(Long userId, Long postId, Long commentId, Long replyId, 
                                       Long senderId, String senderUsername, String senderAvatar,
                                       String replyContent, String parentContent, String postTitle) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type("reply")
                .postId(postId)
                .commentId(commentId)
                .replyId(replyId)
                .senderId(senderId)
                .senderUsername(senderUsername)
                .senderAvatar(senderAvatar)
                .content(replyContent)
                .parentContent(parentContent)
                .postTitle(postTitle)
                .isRead(false)
                .createTime(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        notificationRepository.deleteByUserIdAndId(userId, notificationId);
    }

    /**
     * 创建点赞通知
     */
    @Transactional
    public void createLikeNotification(Long userId, Long postId, Long commentId, Long replyId,
                                      String targetType, Long senderId, String senderUsername, 
                                      String senderAvatar, String content, String postTitle) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type("like")
                .postId(postId)
                .commentId(commentId)
                .replyId(replyId)
                .targetType(targetType)
                .senderId(senderId)
                .senderUsername(senderUsername)
                .senderAvatar(senderAvatar)
                .content(content)
                .postTitle(postTitle)
                .isRead(false)
                .createTime(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }
}
