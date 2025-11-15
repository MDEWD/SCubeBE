package com.scube.scubebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scube.scubebackend.common.ErrorCode;
import com.scube.scubebackend.exception.BusinessException;
import com.scube.scubebackend.mapper.CommentMapper;
import com.scube.scubebackend.mapper.ProductMapper;
import com.scube.scubebackend.mapper.UserMapper;
import com.scube.scubebackend.model.dto.CommentRequest;
import com.scube.scubebackend.model.dto.CommentVO;
import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.PageResult;
import com.scube.scubebackend.model.entity.Comment;
import com.scube.scubebackend.model.entity.Product;
import com.scube.scubebackend.model.entity.User;
import com.scube.scubebackend.service.CommentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO addComment(Long productId, CommentRequest request, LoginUser loginUser) {
        // 检查商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null || product.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        
        Comment comment = new Comment();
        comment.setProductId(productId);
        comment.setUserId(loginUser.getId());
        comment.setContent(request.getContent());
        comment.setRating(request.getRating());
        comment.setLikes(0);
        comment.setDislikes(0);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        comment.setIsDelete(0);
        
        commentMapper.insert(comment);
        
        // 更新商品评分（简化处理，实际应该计算平均值）
        // TODO: 计算平均评分
        
        return convertToVO(comment);
    }
    
    @Override
    public PageResult<CommentVO> getComments(Long productId, Integer page, Integer size) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }
        
        Page<Comment> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getProductId, productId)
                .eq(Comment::getIsDelete, 0)
                .orderByDesc(Comment::getCreateTime);
        
        Page<Comment> commentPage = commentMapper.selectPage(pageParam, queryWrapper);
        
        List<CommentVO> voList = commentPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(
            voList,
            commentPage.getTotal(),
            (long) page,
            (long) size
        );
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voteComment(Long commentId, String action, LoginUser loginUser) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "评论不存在");
        }
        
        if ("like".equals(action)) {
            comment.setLikes(comment.getLikes() + 1);
        } else if ("dislike".equals(action)) {
            comment.setDislikes(comment.getDislikes() + 1);
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "操作类型错误");
        }
        
        comment.setUpdateTime(LocalDateTime.now());
        commentMapper.updateById(comment);
    }
    
    private CommentVO convertToVO(Comment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        
        // 加载用户信息
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            vo.setUserName(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        
        return vo;
    }
}

