package com.scube.scubebackend.service;

import com.scube.scubebackend.model.dto.CommentRequest;
import com.scube.scubebackend.model.dto.CommentVO;
import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.PageResult;

public interface CommentService {
    CommentVO addComment(Long productId, CommentRequest request, LoginUser loginUser);
    PageResult<CommentVO> getComments(Long productId, Integer page, Integer size);
    void voteComment(Long commentId, String action, LoginUser loginUser);
}

