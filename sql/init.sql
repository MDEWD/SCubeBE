-- 算立方（SuanCube）数据库初始化脚本
-- 创建时间: 2024-01-15
-- 数据库: MySQL 8.0+

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS scube CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE scube;

-- ============================================
-- 1. 用户表
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `open_id` VARCHAR(64) NOT NULL COMMENT '微信openId',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `user_role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '用户角色：ADMIN/USER/PARTNER',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_open_id` (`open_id`),
    KEY `idx_user_role` (`user_role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 商品表
-- ============================================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `model` VARCHAR(50) DEFAULT NULL COMMENT '商品型号',
    `gpu_type` VARCHAR(50) DEFAULT NULL COMMENT 'GPU型号',
    `gpu_count` INT NOT NULL DEFAULT 1 COMMENT 'GPU数量',
    `cpu` VARCHAR(100) DEFAULT NULL COMMENT 'CPU型号',
    `memory` VARCHAR(50) DEFAULT NULL COMMENT '内存',
    `system_disk` VARCHAR(50) DEFAULT NULL COMMENT '系统盘',
    `data_disk` VARCHAR(50) DEFAULT NULL COMMENT '数据盘',
    `bandwidth` VARCHAR(50) DEFAULT NULL COMMENT '带宽',
    `max_cuda_version` VARCHAR(20) DEFAULT NULL COMMENT '最大CUDA版本',
    `driver_version` VARCHAR(50) DEFAULT NULL COMMENT '驱动版本',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格（万元）',
    `region` VARCHAR(50) DEFAULT NULL COMMENT '地区',
    `location` VARCHAR(100) DEFAULT NULL COMMENT '数据中心位置',
    `type` VARCHAR(20) NOT NULL DEFAULT 'lease' COMMENT '商品类型：official-recommend/lease',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ACTIVE/INACTIVE/REJECTED',
    `rating` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '评分',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `is_hot` TINYINT NOT NULL DEFAULT 0 COMMENT '是否热门：0-否，1-是',
    `is_new` TINYINT NOT NULL DEFAULT 0 COMMENT '是否新品：0-否，1-是',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_region` (`region`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ============================================
-- 3. 商品标签表
-- ============================================
DROP TABLE IF EXISTS `product_tag`;
CREATE TABLE `product_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品标签表';

-- ============================================
-- 4. 商品图片表
-- ============================================
DROP TABLE IF EXISTS `product_image`;
CREATE TABLE `product_image` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `image_url` VARCHAR(255) NOT NULL COMMENT '图片URL',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品图片表';

-- ============================================
-- 5. 商品应用场景表
-- ============================================
DROP TABLE IF EXISTS `product_application_scene`;
CREATE TABLE `product_application_scene` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `scene_name` VARCHAR(50) NOT NULL COMMENT '场景名称',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品应用场景表';

-- ============================================
-- 6. 评论表
-- ============================================
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content` VARCHAR(500) NOT NULL COMMENT '评论内容',
    `rating` INT NOT NULL COMMENT '评分（1-5）',
    `likes` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `dislikes` INT NOT NULL DEFAULT 0 COMMENT '点踩数',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ============================================
-- 7. 订单表
-- ============================================
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `order_no` VARCHAR(32) NOT NULL COMMENT '订单号',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status` VARCHAR(20) NOT NULL COMMENT '订单状态：PENDING/PAID/ACTIVE/COMPLETED/CANCELLED/REFUNDED',
    `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================
-- 8. 帖子表
-- ============================================
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT NOT NULL COMMENT '内容',
    `vote_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `answer_count` INT NOT NULL DEFAULT 0 COMMENT '回答数',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `accepted_answer_id` BIGINT DEFAULT NULL COMMENT '采纳的回答ID',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    FULLTEXT KEY `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- ============================================
-- 9. 帖子标签表
-- ============================================
DROP TABLE IF EXISTS `post_tag`;
CREATE TABLE `post_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子标签表';

-- ============================================
-- 10. 回答表
-- ============================================
DROP TABLE IF EXISTS `answer`;
CREATE TABLE `answer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `post_id` BIGINT NOT NULL COMMENT '帖子ID',
    `user_id` BIGINT NOT NULL COMMENT '回答者ID',
    `content` TEXT NOT NULL COMMENT '回答内容',
    `vote_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `is_accepted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否采纳：0-否，1-是',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回答表';

-- ============================================
-- 11. 需求表
-- ============================================
DROP TABLE IF EXISTS `requirement`;
CREATE TABLE `requirement` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `title` VARCHAR(200) NOT NULL COMMENT '需求标题',
    `description` TEXT DEFAULT NULL COMMENT '需求描述',
    `gpu_type` VARCHAR(50) DEFAULT NULL COMMENT '所需GPU型号',
    `gpu_count` INT DEFAULT NULL COMMENT '所需GPU数量',
    `budget` DECIMAL(10,2) DEFAULT NULL COMMENT '预算（万元）',
    `deadline` DATE DEFAULT NULL COMMENT '截止日期',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/processing/completed/cancelled',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需求表';

-- ============================================
-- 12. 轮播图表
-- ============================================
DROP TABLE IF EXISTS `carousel`;
CREATE TABLE `carousel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
    `image` VARCHAR(255) DEFAULT NULL COMMENT '图片URL',
    `link` VARCHAR(255) DEFAULT NULL COMMENT '链接',
    `order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播图表';

-- ============================================
-- 13. 服务保障表
-- ============================================
DROP TABLE IF EXISTS `service`;
CREATE TABLE `service` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务保障表';

