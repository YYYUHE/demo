package com.example.demo.service;

import com.example.demo.entity.Follow;
import com.example.demo.entity.User;
import com.example.demo.repository.FollowRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关注服务类
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * 关注用户
     */
    @Transactional
    public void followUser(Long followerId, Long followingId) {
        // 不能关注自己
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }

        // 检查被关注者是否存在
        if (!userRepository.existsById(followingId)) {
            throw new RuntimeException("用户不存在");
        }

        // 检查是否已经关注
        if (followRepository.findByFollowerIdAndFollowingId(followerId, followingId).isPresent()) {
            throw new RuntimeException("已经关注该用户");
        }

        // 创建关注关系
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .build();

        followRepository.save(follow);
    }

    /**
     * 取消关注
     */
    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * 切换关注状态（关注/取消关注）
     */
    @Transactional
    public Map<String, Object> toggleFollow(Long followerId, Long followingId) {
        // 不能关注自己
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("不能关注自己");
        }

        // 检查被关注者是否存在
        if (!userRepository.existsById(followingId)) {
            throw new IllegalArgumentException("用户不存在");
        }

        Map<String, Object> result = new HashMap<>();

        Optional<Follow> existing = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        if (existing.isPresent()) {
            // 已关注，取消关注
            followRepository.delete(existing.get());
            result.put("following", false);
        } else {
            // 未关注，进行关注
            Follow follow = Follow.builder()
                    .followerId(followerId)
                    .followingId(followingId)
                    .build();
            followRepository.save(follow);
            result.put("following", true);
        }

        // 返回最新的粉丝数
        result.put("followerCount", followRepository.countByFollowingId(followingId));

        return result;
    }

    /**
     * 检查是否已关注
     */
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.findByFollowerIdAndFollowingId(followerId, followingId).isPresent();
    }

    /**
     * 获取用户关注的所有用户ID
     */
    public List<Long> getFollowingIds(Long userId) {
        return followRepository.findFollowingIdsByFollowerId(userId);
    }

    /**
     * 获取用户的粉丝ID列表
     */
    public List<Long> getFollowerIds(Long userId) {
        return followRepository.findFollowerIdsByFollowingId(userId);
    }

    /**
     * 获取用户关注的人数
     */
    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    /**
     * 获取用户的粉丝数
     */
    public long getFollowerCount(Long userId) {
        return followRepository.countByFollowingId(userId);
    }

    /**
     * 获取用户关注的人的详细信息
     */
    public List<User> getFollowingUsers(Long userId) {
        List<Long> followingIds = getFollowingIds(userId);
        if (followingIds.isEmpty()) {
            return new ArrayList<>();
        }
        return userRepository.findAllById(followingIds);
    }

    /**
     * 获取用户关注的人的详细信息（包含是否已关注标志）
     */
    public List<Map<String, Object>> getFollowingUsersWithDetails(Long userId, Long currentUserId) {
        List<User> followingUsers = getFollowingUsers(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (User user : followingUsers) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("avatar", user.getAvatar());
            userInfo.put("following", currentUserId != null && isFollowing(currentUserId, user.getId()));
            result.add(userInfo);
        }

        return result;
    }
}
