package com.scube.scubebackend.controller;

import com.scube.scubebackend.model.dto.BaseResponse;
import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.PageResult;
import com.scube.scubebackend.model.dto.PostDetailVO;
import com.scube.scubebackend.model.dto.PostRequest;
import com.scube.scubebackend.model.dto.PostVO;
import com.scube.scubebackend.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/post")
public class PostController extends BaseController {
    
    @Autowired
    private PostService postService;
    
    @PostMapping
    public BaseResponse<PostVO> createPost(@RequestBody @Valid PostRequest request) {
        LoginUser loginUser = getLoginUser();
        PostVO post = postService.createPost(request, loginUser);
        return BaseResponse.success("发布成功", post);
    }
    
    @GetMapping("/list")
    public BaseResponse<PageResult<PostVO>> getPostList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        PageResult<PostVO> result = postService.getPostList(keyword, tag, page, size);
        return BaseResponse.success(result);
    }
    
    @GetMapping("/{id}")
    public BaseResponse<PostDetailVO> getPostById(@PathVariable Long id) {
        PostDetailVO post = postService.getPostById(id);
        return BaseResponse.success(post);
    }
}

