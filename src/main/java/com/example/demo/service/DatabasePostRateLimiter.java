package com.example.demo.service;

import com.example.demo.entity.Post;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DatabasePostRateLimiter implements PostRateLimiter {

    private static final long LIMIT_SECONDS = 60L;
    private final PostRepository postRepository;

    @Override
    public void validateCanPublish(Long userId) {
        Post latest = postRepository.findTopByAuthorIdOrderByCreateTimeDesc(userId).orElse(null);
        if (latest == null || latest.getCreateTime() == null) {
            return;
        }
        long diff = Duration.between(latest.getCreateTime(), LocalDateTime.now()).getSeconds();
        if (diff < LIMIT_SECONDS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, 429, "发帖过于频繁，请稍后再试");
        }
    }
}
