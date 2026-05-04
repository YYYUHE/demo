package com.example.demo.service;

public interface PostRateLimiter {
    void validateCanPublish(Long userId);
}
