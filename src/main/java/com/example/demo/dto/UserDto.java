package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    
    private Long id;
    
    private String uid;
    
    private String username;
    
    private String password;
    
    private String avatar;
}
