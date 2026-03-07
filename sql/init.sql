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
    `display_id` VARCHAR(64) NOT NULL COMMENT '显示ID',
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
    `user_id` BIGINT NOT NULL COMMENT '发布者ID(关联用户表的id字段)',
    `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `model` VARCHAR(50) DEFAULT NULL COMMENT '商品型号',
    `product_id` VARCHAR(12) NOT NULL COMMENT '商品展示ID',
    `gpu_type` VARCHAR(50) DEFAULT NULL COMMENT 'GPU型号',
    `stock` INT DEFAULT NULL COMMENT '库存',
    `gpu_count` INT NOT NULL DEFAULT 1 COMMENT 'GPU数量',
    `cpu` VARCHAR(100) DEFAULT NULL COMMENT 'CPU型号',
    `memory` VARCHAR(50) DEFAULT NULL COMMENT '内存',
    `system_disk` VARCHAR(50) DEFAULT NULL COMMENT '系统盘',
    `storage` VARCHAR(50) DEFAULT NULL COMMENT '数据存储',
    `bandwidth` VARCHAR(50) DEFAULT NULL COMMENT '带宽',
    `max_cuda_version` VARCHAR(20) DEFAULT NULL COMMENT '最大CUDA版本',
    `driver_version` VARCHAR(50) DEFAULT NULL COMMENT '驱动版本',
    `monthly_price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `region` VARCHAR(50) DEFAULT NULL COMMENT '地区',
    `position` VARCHAR(100) DEFAULT NULL COMMENT '数据中心位置',
    `type` VARCHAR(20) NOT NULL DEFAULT 'lease' COMMENT '商品类型：official-recommend/lease',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ACTIVE/INACTIVE/REJECTED',
    `rating` DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '评分',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    `is_hot` TINYINT NOT NULL DEFAULT 0 COMMENT '是否热门：0-否，1-是',
    `is_new` TINYINT NOT NULL DEFAULT 0 COMMENT '是否新品：0-否，1-是',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    `high_speed_net_card` INT NULL COMMENT '高速网卡',
    `tag` VARCHAR(100) DEFAULT NULL COMMENT '标签',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_id` (`product_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_status` (`status`),
    KEY `idx_region` (`region`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';
-- ============================================
-- 3. 合作伙伴申请表
-- ============================================
DROP TABLE IF EXISTS `alliance_application`;
CREATE TABLE `alliance_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `user_display_id` VARCHAR(64) NOT NULL COMMENT '用户展示ID',
    `kind` VARCHAR(20) NOT NULL COMMENT '申请类型：person/company',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
    `id_number` VARCHAR(30) DEFAULT NULL COMMENT '身份证号',
    `id_front_image` LONGTEXT DEFAULT NULL COMMENT '身份证正面图片(base64)',
    `id_back_image` LONGTEXT DEFAULT NULL COMMENT '身份证反面图片(base64)',
    `org_name` VARCHAR(100) DEFAULT NULL COMMENT '企业/学校名称',
    `credit_code` VARCHAR(50) DEFAULT NULL COMMENT '统一社会信用代码',
    `license_image` TEXT DEFAULT NULL COMMENT '营业执照图片(base64)',
    `job` VARCHAR(20) DEFAULT NULL COMMENT '申请人职务',
    `main_business` VARCHAR(200) DEFAULT NULL COMMENT '主营业务',
    `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人姓名',
    `contact_method` VARCHAR(100) DEFAULT NULL COMMENT '联系方式',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
    `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    `is_delete` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_kind` (`kind`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合作伙伴申请表';
