package com.scube.scubebackend.service;

import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.PageResult;
import com.scube.scubebackend.model.dto.PostDetailVO;
import com.scube.scubebackend.model.dto.PostRequest;
import com.scube.scubebackend.model.dto.PostVO;

public interface PostService {
    PostVO createPost(PostRequest request, LoginUser loginUser);
    PageResult<PostVO> getPostList(String keyword, String tag, Integer page, Integer size);
    PostDetailVO getPostById(Long id);
}

