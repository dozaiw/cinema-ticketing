CREATE TABLE IF NOT EXISTS `comment_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '举报 ID',
  `comment_id` BIGINT NOT NULL COMMENT '被举报评论 ID',
  `reporter_id` INT NOT NULL COMMENT '举报人 ID',
  `reporter_nickname` VARCHAR(64) DEFAULT NULL COMMENT '举报人昵称快照',
  `reason_type` VARCHAR(32) NOT NULL COMMENT '举报原因',
  `report_content` VARCHAR(255) DEFAULT NULL COMMENT '补充说明',
  `report_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1驳回举报 2评论已删除',
  `report_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  `admin_remark` VARCHAR(255) DEFAULT NULL COMMENT '管理员备注',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handled_by` INT DEFAULT NULL COMMENT '处理人 ID',
  PRIMARY KEY (`id`),
  KEY `idx_comment_report_comment_id` (`comment_id`),
  KEY `idx_comment_report_reporter_id` (`reporter_id`),
  KEY `idx_comment_report_status` (`report_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论举报表';
