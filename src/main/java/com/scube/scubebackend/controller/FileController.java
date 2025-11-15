package com.scube.scubebackend.controller;

import com.scube.scubebackend.model.dto.BaseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {
    
    @Value("${file.upload.path:./uploads}")
    private String uploadPath;
    
    @Value("${file.upload.url:http://localhost:8077/uploads}")
    private String uploadUrl;
    
    @PostMapping("/upload")
    public BaseResponse<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return BaseResponse.error(40000, "文件不能为空");
        }
        
        // 检查文件类型
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        if (!extension.matches("(?i)\\.(jpg|jpeg|png|gif)")) {
            return BaseResponse.error(40000, "只支持jpg、png、gif格式的图片");
        }
        
        // 检查文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return BaseResponse.error(40000, "文件大小不能超过10MB");
        }
        
        try {
            // 创建目录
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path dirPath = Paths.get(uploadPath, dateDir);
            Files.createDirectories(dirPath);
            
            // 生成文件名
            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = dirPath.resolve(fileName);
            
            // 保存文件
            file.transferTo(filePath.toFile());
            
            // 返回URL
            String url = uploadUrl + "/" + dateDir + "/" + fileName;
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            
            return BaseResponse.success(result);
        } catch (IOException e) {
            return BaseResponse.error(50000, "文件上传失败：" + e.getMessage());
        }
    }
}

