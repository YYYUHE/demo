package com.example.demo.service;

import com.example.demo.dto.CastVoteRequest;
import com.example.demo.dto.CreateVoteRequest;
import com.example.demo.dto.VoteDto;
import com.example.demo.entity.*;
import com.example.demo.repository.PostRepository;
import com.example.demo.repository.VoteOptionRepository;
import com.example.demo.repository.VoteRecordRepository;
import com.example.demo.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;
    @Mock
    private VoteOptionRepository voteOptionRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private VoteRecordRepository voteRecordRepository;

    @InjectMocks
    private VoteService voteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateVote() {
        Long postId = 1L;
        CreateVoteRequest request = new CreateVoteRequest();
        request.setTitle("Test Vote");
        request.setOptions(Arrays.asList("Option 1", "Option 2"));
        request.setMaxChoices(1);

        Post post = new Post();
        post.setId(postId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(voteRepository.findByPostId(postId)).thenReturn(Optional.empty());
        when(voteRepository.save(any(Vote.class))).thenAnswer(i -> {
            Vote v = i.getArgument(0);
            v.setId(100L);
            return v;
        });

        VoteDto result = voteService.createVote(postId, request);

        assertNotNull(result);
        assertEquals("Test Vote", result.getTitle());
        assertEquals(2, result.getOptions().size());
        verify(voteRepository, times(1)).save(any(Vote.class));
        verify(postRepository, times(1)).save(any(Post.class));
    }

    @Test
    void testCastVote() {
        User user = new User();
        user.setId(1L);
        
        Vote vote = new Vote();
        vote.setId(10L);
        vote.setMaxChoices(1);
        vote.setTotalVoters(0);
        vote.setIsEnded(false);
        
        VoteOption option = new VoteOption();
        option.setId(101L);
        option.setVote(vote);
        option.setVoteCount(0);
        vote.setOptions(Arrays.asList(option));

        CastVoteRequest request = new CastVoteRequest();
        request.setVoteId(10L);
        request.setOptionIds(Arrays.asList(101L));

        when(voteRepository.findById(10L)).thenReturn(Optional.of(vote));
        when(voteRecordRepository.findByVoteIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        when(voteRecordRepository.findByVoteIdAndIp(any(), any())).thenReturn(Optional.empty());
        when(voteOptionRepository.findByVoteIdOrderBySortOrderAsc(10L)).thenReturn(Arrays.asList(option));

        voteService.castVote(request, user, "127.0.0.1");

        assertEquals(1, vote.getTotalVoters());
        assertEquals(1, option.getVoteCount());
        verify(voteRecordRepository, times(1)).save(any(VoteRecord.class));
    }

    @Test
    void testCastVoteDuplicate() {
        User user = new User();
        user.setId(1L);
        
        Vote vote = new Vote();
        vote.setId(10L);

        CastVoteRequest request = new CastVoteRequest();
        request.setVoteId(10L);

        when(voteRepository.findById(10L)).thenReturn(Optional.of(vote));
        when(voteRecordRepository.findByVoteIdAndUserId(10L, 1L)).thenReturn(Optional.of(new VoteRecord()));

        assertThrows(RuntimeException.class, () -> {
            voteService.castVote(request, user, "127.0.0.1");
        });
    }
}
