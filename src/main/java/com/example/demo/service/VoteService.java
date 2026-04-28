package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.VoteOptionRepository;
import com.example.demo.repository.VoteRecordRepository;
import com.example.demo.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final VoteOptionRepository voteOptionRepository;
    private final PostRepository postRepository;
    private final VoteRecordRepository voteRecordRepository;

    /**
     * 创建投票
     */
    @Transactional
    public VoteDto createVote(Long postId, CreateVoteRequest request) {
        // 验证帖子是否存在
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));

        // 验证选项数量
        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new RuntimeException("至少需要2个投票选项");
        }

        // 验证maxChoices
        int maxChoices = request.getMaxChoices() != null ? request.getMaxChoices() : 1;
        if (maxChoices < 1 || maxChoices > request.getOptions().size()) {
            throw new RuntimeException("可选项目数不合法");
        }

        // 检查是否已有投票
        if (voteRepository.findByPostId(postId).isPresent()) {
            throw new RuntimeException("该帖子已有投票，无法重复创建");
        }

        // 创建投票
        Vote vote = Vote.builder()
                .postId(postId)
                .title(request.getTitle())
                .deadline(request.getDeadline())
                .maxChoices(maxChoices)
                .totalVoters(0)
                .isEnded(false)
                .options(new ArrayList<>())  // 显式初始化 options 列表，防止 NullPointerException
                .build();

        // 添加选项
        List<String> options = request.getOptions();
        for (int i = 0; i < options.size(); i++) {
            VoteOption option = VoteOption.builder()
                    .content(options.get(i))
                    .sortOrder(i)
                    .voteCount(0)
                    .build();
            vote.addOption(option);
        }

        Vote savedVote = voteRepository.save(vote);

        // 更新帖子的投票ID
        post.setVoteId(savedVote.getId());
        postRepository.save(post);

        return convertToDto(savedVote, null);
    }

    /**
     * 获取帖子的投票信息
     */
    public VoteDto getVoteByPostId(Long postId, User currentUser) {
        Vote vote = voteRepository.findByPostId(postId)
                .orElse(null);

        if (vote == null) {
            return null;
        }

        // 检查投票是否已过期
        if (!vote.getIsEnded() && vote.checkIfExpired()) {
            vote.setIsEnded(true);
            voteRepository.save(vote);
        }

        return convertToDto(vote, currentUser);
    }

    /**
     * 根据投票ID获取投票信息
     */
    public VoteDto getVoteById(Long voteId, User currentUser) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new RuntimeException("投票不存在"));

        // 检查投票是否已过期
        if (!vote.getIsEnded() && vote.checkIfExpired()) {
            vote.setIsEnded(true);
            voteRepository.save(vote);
        }

        return convertToDto(vote, currentUser);
    }

    /**
     * 提交投票
     */
    @Transactional
    public void castVote(CastVoteRequest request, User currentUser, String ip) {
        if (currentUser == null) {
            throw new RuntimeException("请先登录");
        }

        // 获取投票
        Vote vote = voteRepository.findById(request.getVoteId())
                .orElseThrow(() -> new RuntimeException("投票不存在"));

        // 检查投票是否已结束
        if (vote.getIsEnded() || vote.checkIfExpired()) {
            throw new RuntimeException("投票已结束");
        }

        // 1. 检查同一用户是否已投过票
        if (voteRecordRepository.findByVoteIdAndUserId(vote.getId(), currentUser.getId()).isPresent()) {
            throw new RuntimeException("您已经投过票了，不能重复投票");
        }

        // 2. 检查同一IP是否已投过票（防刷）- 暂时禁用用于测试
        // if (ip != null && voteRecordRepository.findByVoteIdAndIp(vote.getId(), ip).isPresent()) {
        //     throw new RuntimeException("同一IP只能投票一次");
        // }

        // 验证选择的选项数量
        List<Long> optionIds = request.getOptionIds();
        if (optionIds == null || optionIds.isEmpty()) {
            throw new RuntimeException("请至少选择一个选项");
        }
        if (optionIds.size() > vote.getMaxChoices()) {
            throw new RuntimeException("最多只能选择" + vote.getMaxChoices() + "项");
        }

        // 获取所有选项
        List<VoteOption> options = voteOptionRepository.findByVoteIdOrderBySortOrderAsc(vote.getId());

        // 验证选项是否属于该投票
        for (Long optionId : optionIds) {
            boolean valid = options.stream().anyMatch(opt -> opt.getId().equals(optionId));
            if (!valid) {
                throw new RuntimeException("无效的选项");
            }
        }

        // 为每个选中的选项添加投票
        for (Long optionId : optionIds) {
            VoteOption option = options.stream()
                    .filter(opt -> opt.getId().equals(optionId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("选项不存在"));

            option.addVoter(currentUser.getId());
        }

        // 更新总投票人数
        vote.setTotalVoters(vote.getTotalVoters() + 1);
        voteRepository.save(vote);

        // 保存选项的投票记录
        voteOptionRepository.saveAll(options);

        // 保存防刷记录
        VoteRecord record = VoteRecord.builder()
                .voteId(vote.getId())
                .userId(currentUser.getId())
                .ip(ip != null ? ip : "unknown")
                .build();
        voteRecordRepository.save(record);
    }

    /**
     * 删除投票
     */
    @Transactional
    public void deleteVote(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new RuntimeException("投票不存在"));

        // 删除关联的选项
        voteOptionRepository.deleteByVoteId(voteId);

        // 删除投票
        voteRepository.delete(vote);

        // 清除帖子的投票ID
        Post post = postRepository.findById(vote.getPostId())
                .orElse(null);
        if (post != null) {
            post.setVoteId(null);
            postRepository.save(post);
        }
    }

    /**
     * 转换为DTO
     */
    private VoteDto convertToDto(Vote vote, User currentUser) {
        VoteDto dto = new VoteDto();
        dto.setId(vote.getId());
        dto.setPostId(vote.getPostId());
        dto.setTitle(vote.getTitle());
        dto.setDeadline(vote.getDeadline());
        dto.setMaxChoices(vote.getMaxChoices());
        dto.setTotalVoters(vote.getTotalVoters());
        dto.setIsEnded(vote.getIsEnded());
        dto.setCreateTime(vote.getCreateTime());
        dto.setUpdateTime(vote.getUpdateTime());

        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        boolean hasVoted = false;
        if (currentUserId != null) {
            hasVoted = vote.getOptions().stream().anyMatch(o -> o.hasVoted(currentUserId));
        }
        dto.setHasVoted(hasVoted);

        // 如果未投票且投票未结束，隐藏统计数据（按需求：在用户投票前完全隐藏投票统计数据）
        // 但如果投票已结束，通常应该显示结果
        boolean showResults = hasVoted || vote.getIsEnded();

        // 转换选项列表
        List<VoteOptionDto> optionDtos = vote.getOptions().stream()
                .map(option -> {
                    VoteOptionDto optionDto = new VoteOptionDto();
                    optionDto.setId(option.getId());
                    optionDto.setVoteId(vote.getId());
                    optionDto.setContent(option.getContent());
                    optionDto.setSortOrder(option.getSortOrder());
                    
                    if (showResults) {
                        optionDto.setVoteCount(option.getVoteCount());
                        if (vote.getTotalVoters() > 0) {
                            optionDto.setPercentage((double) option.getVoteCount() / vote.getTotalVoters() * 100);
                        } else {
                            optionDto.setPercentage(0.0);
                        }
                    } else {
                        optionDto.setVoteCount(null);
                        optionDto.setPercentage(null);
                    }

                    if (currentUserId != null) {
                        optionDto.setVoted(option.hasVoted(currentUserId));
                    } else {
                        optionDto.setVoted(false);
                    }
                    return optionDto;
                })
                .collect(Collectors.toList());

        dto.setOptions(optionDtos);

        return dto;
    }
}
