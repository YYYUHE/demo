package com.example.demo.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum PostCategory {
    FRIEND("交友"),
    SECOND_HAND("二手"),
    CHAT("闲聊"),
    LOST_FOUND("失物招领");

    private final String label;

    PostCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Set<String> whitelist() {
        return Arrays.stream(values()).map(PostCategory::getLabel).collect(Collectors.toSet());
    }
}
