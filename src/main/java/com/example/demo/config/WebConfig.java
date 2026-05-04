package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:uploads}")
    private String uploadPath;
    
    private final AuthInterceptor authInterceptor;

    /**
     * 配置静态资源映射
     * 将 /uploads/** 请求映射到实际的文件存储路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
    
    /**
     * 配置拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                    "/index.html",
                    "/app.html",
                    "/app",
                    "/post-editor.html",
                    "/posts.html",
                    "/drafts.html",
                    "/favorites.html",
                    "/my-posts.html",
                    "/messages.html",
                    "/post/**",
                    "/profile",
                    "/favorites",
                    "/my-posts",
                    "/messages"
                )
                .excludePathPatterns(
                    "/auth.html",
                    "/api/auth/**",
                    "/api/posts/**",
                    "/api/drafts/**",
                    "/api/files/**",
                    "/api/mentions/**",
                    "/api/comments/**",
                    "/api/topics/**",
                    "/api/follows/**",
                    "/api/messages/**"
                );
    }
}
