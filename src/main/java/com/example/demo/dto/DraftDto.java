package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftDto {
    private Long id;
    private List<Long> ids;
    private String title;
    private String draftType;
    private String content;
}
