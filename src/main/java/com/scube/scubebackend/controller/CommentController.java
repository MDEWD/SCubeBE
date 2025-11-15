package com.scube.scubebackend.controller;

import com.scube.scubebackend.model.dto.BaseResponse;
import com.scube.scubebackend.model.dto.CommentRequest;
import com.scube.scubebackend.model.dto.CommentVO;
import com.scube.scubebackend.model.dto.LoginUser;
import com.scube.scubebackend.model.dto.PageResult;
import com.scube.scubebackend.model.dto.VoteRequest;
import com.scube.scubebackend.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
public class CommentController extends BaseController {
    
    @Autowired
    private CommentService commentService;
    
    @PostMapping("/{productId}/comment")
    public BaseResponse<CommentVO> addComment(@PathVariable Long productId,
                                              @RequestBody @Valid CommentRequest request) {
        LoginUser loginUser = getLoginUser();
        CommentVO comment = commentService.addComment(productId, request, loginUser);
        return BaseResponse.success("评论成功", comment);
    }
    
    @GetMapping("/{productId}/comments")
    public BaseResponse<PageResult<CommentVO>> getComments(@PathVariable Long productId,
                                                           @RequestParam(required = false, defaultValue = "1") Integer page,
                                                           @RequestParam(required = false, defaultValue = "10") Integer size) {
        PageResult<CommentVO> result = commentService.getComments(productId, page, size);
        return BaseResponse.success(result);
    }
    
    @PostMapping("/comment/{commentId}/vote")
    public BaseResponse<Void> voteComment(@PathVariable Long commentId,
                                          @RequestBody @Valid VoteRequest request) {
        LoginUser loginUser = getLoginUser();
        commentService.voteComment(commentId, request.getAction(), loginUser);
        return BaseResponse.success("操作成功", null);
    }
}

