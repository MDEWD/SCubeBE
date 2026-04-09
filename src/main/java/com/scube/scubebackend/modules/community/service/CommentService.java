package com.scube.scubebackend.modules.community.service;

import com.scube.scubebackend.modules.community.model.dto.CommentRequest;
import com.scube.scubebackend.modules.community.model.dto.CommentVO;
import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.common.model.dto.PageResult;

public interface CommentService {
    CommentVO addComment(Long productId, CommentRequest request, LoginUser loginUser);
    PageResult<CommentVO> getComments(Long productId, Integer page, Integer size);
    void voteComment(Long commentId, String action, LoginUser loginUser);
}

