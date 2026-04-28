package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({
            "/",
            "/auth",
            "/posts",
            "/post-editor",
            "/drafts",
            "/profile",
            "/favorites",
            "/my-posts",
            "/messages",
            "/messages/**"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
