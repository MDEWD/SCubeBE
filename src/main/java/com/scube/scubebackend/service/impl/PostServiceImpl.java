package com.scube.scubebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.mapper.AnswerMapper;
import com.scube.scubebackend.mapper.PostMapper;
import com.scube.scubebackend.mapper.PostTagMapper;
import com.scube.scubebackend.mapper.UserMapper;
import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.PageResult;
import com.scube.scubebackend.model.dto.PostDetailVO;
import com.scube.scubebackend.model.dto.PostRequest;
import com.scube.scubebackend.model.dto.PostVO;
import com.scube.scubebackend.model.entity.Answer;
import com.scube.scubebackend.model.entity.Post;
import com.scube.scubebackend.model.entity.PostTag;
import com.scube.scubebackend.model.entity.User;
import com.scube.scubebackend.service.PostService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private PostTagMapper postTagMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private AnswerMapper answerMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO createPost(PostRequest request, LoginUser loginUser) {
        Post post = new Post();
        post.setUserId(loginUser.getId());
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setVoteCount(0);
        post.setAnswerCount(0);
        post.setViewCount(0);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        post.setIsDelete(0);
        
        postMapper.insert(post);
        
        // 插入标签
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tag : request.getTags()) {
                PostTag postTag = new PostTag();
                postTag.setPostId(post.getId());
                postTag.setTagName(tag);
                postTag.setCreateTime(LocalDateTime.now());
                postTagMapper.insert(postTag);
            }
        }
        
        return convertToVO(post);
    }
    
    @Override
    public PageResult<PostVO> getPostList(String keyword, String tag, Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        
        Page<Post> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Post::getIsDelete, 0);
        
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like(Post::getTitle, keyword)
                .or()
                .like(Post::getContent, keyword)
            );
        }
        
        queryWrapper.orderByDesc(Post::getCreateTime);
        
        Page<Post> postPage = postMapper.selectPage(pageParam, queryWrapper);
        
        // 如果指定了标签，需要过滤
        List<PostVO> voList = postPage.getRecords().stream()
                .map(this::convertToVO)
                .filter(postVO -> {
                    if (tag != null && !tag.isEmpty()) {
                        return postVO.getTags() != null && postVO.getTags().contains(tag);
                    }
                    return true;
                })
                .collect(Collectors.toList());
        
        return new PageResult<>(
            voList,
            postPage.getTotal(),
            (long) page,
            (long) size
        );
    }
    
    @Override
    public PostDetailVO getPostById(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || post.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        
        // 更新浏览量
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);
        
        PostDetailVO vo = new PostDetailVO();
        BeanUtils.copyProperties(post, vo);
        
        // 加载用户信息
        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            vo.setAuthorName(user.getNickname());
        }
        
        // 加载标签
        LambdaQueryWrapper<PostTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(PostTag::getPostId, id);
        List<PostTag> tags = postTagMapper.selectList(tagWrapper);
        vo.setTags(tags.stream().map(PostTag::getTagName).collect(Collectors.toList()));
        
        // 加载回答
        LambdaQueryWrapper<Answer> answerWrapper = new LambdaQueryWrapper<>();
        answerWrapper.eq(Answer::getPostId, id)
                .eq(Answer::getIsDelete, 0)
                .orderByDesc(Answer::getVoteCount)
                .orderByDesc(Answer::getCreateTime);
        List<Answer> answers = answerMapper.selectList(answerWrapper);
        
        vo.setAnswers(answers.stream().map(answer -> {
            com.scube.scubebackend.model.dto.AnswerVO answerVO = new com.scube.scubebackend.model.dto.AnswerVO();
            BeanUtils.copyProperties(answer, answerVO);
            answerVO.setIsAccepted(answer.getIsAccepted() == 1);
            
            User answerUser = userMapper.selectById(answer.getUserId());
            if (answerUser != null) {
                answerVO.setAuthorName(answerUser.getNickname());
            }
            
            return answerVO;
        }).collect(Collectors.toList()));
        
        return vo;
    }
    
    private PostVO convertToVO(Post post) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);
        
        // 加载用户信息
        User user = userMapper.selectById(post.getUserId());
        if (user != null) {
            vo.setAuthorName(user.getNickname());
            vo.setAuthorAvatar(user.getAvatar());
        }
        
        // 加载标签
        LambdaQueryWrapper<PostTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(PostTag::getPostId, post.getId());
        List<PostTag> tags = postTagMapper.selectList(tagWrapper);
        vo.setTags(tags.stream().map(PostTag::getTagName).collect(Collectors.toList()));
        
        return vo;
    }
}

