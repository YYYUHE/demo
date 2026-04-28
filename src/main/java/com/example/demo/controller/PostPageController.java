package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PostPageController {
    @GetMapping("/post/{id}")
    public String postDetailPage(@PathVariable Long id) {
        return "forward:/index.html";
    }
}
