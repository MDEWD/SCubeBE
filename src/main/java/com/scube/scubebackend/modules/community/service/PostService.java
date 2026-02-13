package com.scube.scubebackend.modules.community.service;

import com.scube.scubebackend.modules.user.model.dto.LoginUser;
import com.scube.scubebackend.common.model.dto.PageResult;
import com.scube.scubebackend.modules.community.model.dto.PostDetailVO;
import com.scube.scubebackend.modules.community.model.dto.PostRequest;
import com.scube.scubebackend.modules.community.model.dto.PostVO;

public interface PostService {
    PostVO createPost(PostRequest request, LoginUser loginUser);
    PageResult<PostVO> getPostList(String keyword, String tag, Integer page, Integer size);
    PostDetailVO getPostById(Long id);
}

