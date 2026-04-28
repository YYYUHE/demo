package com.example.demo.service;

import com.example.demo.dto.DraftDto;
import com.example.demo.entity.Draft;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.DraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DraftService {

    private final DraftRepository draftRepository;

    /**
     * 保存草稿（新增或更新）
     */
    @Transactional
    public Draft saveDraft(DraftDto draftDto, Long userId) {
        Draft draft;
        
        if (draftDto.getId() != null) {
            draft = draftRepository.findByIdAndUserId(draftDto.getId(), userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 404, "草稿不存在"));
            draft.setTitle(draftDto.getTitle());
            draft.setDraftType(normalizeDraftType(draftDto.getDraftType(), draft.getDraftType()));
            draft.setContent(draftDto.getContent());
        } else {
            draft = Draft.builder()
                    .title(draftDto.getTitle())
                    .draftType(normalizeDraftType(draftDto.getDraftType(), null))
                    .content(draftDto.getContent())
                    .userId(userId)
                    .build();
        }
        
        return draftRepository.save(draft);
    }

    private String normalizeDraftType(String draftType, String fallback) {
        String t = draftType == null ? "" : draftType.trim().toLowerCase();
        if ("image".equals(t)) return "image";
        if ("text".equals(t)) return "text";
        String fb = fallback == null ? "" : fallback.trim().toLowerCase();
        if ("image".equals(fb)) return "image";
        return "text";
    }

    /**
     * 获取所有草稿（按更新时间倒序）
     */
    public List<Draft> getAllDrafts(Long userId) {
        return draftRepository.findByUserIdOrderByUpdateTimeDesc(userId);
    }

    /**
     * 根据ID获取草稿
     */
    public Draft getDraftById(Long id, Long userId) {
        return draftRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 404, "草稿不存在"));
    }

    /**
     * 删除草稿
     */
    @Transactional
    public void deleteDraft(Long id, Long userId) {
        if (!draftRepository.existsByIdAndUserId(id, userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, 404, "草稿不存在");
        }
        draftRepository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    public void batchDeleteDrafts(List<Long> ids, Long userId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        draftRepository.deleteByIdInAndUserId(ids, userId);
    }

    /**
     * 发布后删除草稿
     */
    @Transactional
    public void deleteAfterPublish(Long id, Long userId) {
        if (id != null && draftRepository.existsByIdAndUserId(id, userId)) {
            draftRepository.deleteByIdAndUserId(id, userId);
        }
    }
}
