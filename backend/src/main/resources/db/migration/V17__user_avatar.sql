-- 用户头像 URL（本地上传后存完整可访问地址或相对路径，演示存完整 URL）
ALTER TABLE `user` ADD COLUMN `avatar_url` VARCHAR(512) NULL COMMENT '头像 URL' AFTER `nickname`;
