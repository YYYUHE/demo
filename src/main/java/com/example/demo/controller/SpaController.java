package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/app", "/auth", "/posts", "/profile", "/favorites", "/my-posts", "/drafts", "/post-editor", "/messages"})
    public String spaRoutes() {
        return "forward:/app.html";
    }
}
