CREATE TABLE t_activity (
                            id INT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
                            title VARCHAR(100) NOT NULL COMMENT '活动标题',
                            start_time DATETIME NOT NULL COMMENT '开始时间',
                            end_time DATETIME NOT NULL COMMENT '结束时间',
                            type TINYINT NOT NULL COMMENT '活动类型：1=概率型，2=固定时间型，3=先到先得型',
                            status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=未开始，1=进行中，2=已结束',
                            probability INT DEFAULT 0 COMMENT '概率型活动的中奖概率(0-100)',
                            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            INDEX idx_status_type (status, type),
                            INDEX idx_time (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖活动表';

CREATE TABLE t_prize (
                         id INT AUTO_INCREMENT PRIMARY KEY COMMENT '奖品ID',
                         name VARCHAR(100) NOT NULL COMMENT '奖品名称',
                         price DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '奖品价值',
                         total_amount INT NOT NULL COMMENT '初始总数量',
                         remaining_amount INT NOT NULL COMMENT '剩余数量',
                         image_url VARCHAR(255) DEFAULT NULL COMMENT '奖品图片URL',
                         create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         INDEX idx_remaining (remaining_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖品表';

CREATE TABLE t_activity_prize (
                                  id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
                                  activity_id INT NOT NULL COMMENT '活动ID',
                                  prize_id INT NOT NULL COMMENT '奖品ID',
                                  amount INT NOT NULL COMMENT '该活动分配的奖品数量',
                                  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  UNIQUE KEY uk_activity_prize (activity_id, prize_id),
                                  INDEX idx_activity (activity_id),
                                  INDEX idx_prize (prize_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动奖品关联表';

CREATE TABLE t_activity_rule (
                                 id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
                                 activity_id INT NOT NULL COMMENT '活动ID',
                                 user_level INT NOT NULL DEFAULT 0 COMMENT '用户等级（0表示所有等级）',
                                 max_draws_daily INT NOT NULL DEFAULT 0 COMMENT '每日最大抽奖次数（0表示不限制）',
                                 max_wins_daily INT NOT NULL DEFAULT 0 COMMENT '每日最大中奖次数（0表示不限制）',
                                 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 UNIQUE KEY uk_activity_level (activity_id, user_level),
                                 INDEX idx_activity (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动规则表';

CREATE TABLE t_token (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '令牌ID',
                         activity_id INT NOT NULL COMMENT '活动ID',
                         prize_id INT NOT NULL COMMENT '奖品ID',
                         token_timestamp BIGINT NOT NULL COMMENT '令牌时间戳',
                         status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=未使用，1=已使用',
                         create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         INDEX idx_activity_status (activity_id, status),
                         INDEX idx_timestamp (token_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动令牌表';

CREATE TABLE t_user_draw_record (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
                                    user_id INT NOT NULL COMMENT '用户ID',
                                    activity_id INT NOT NULL COMMENT '活动ID',
                                    draw_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '抽奖时间',
                                    draw_date DATE GENERATED ALWAYS AS (DATE(draw_time)) STORED COMMENT '抽奖日期',
                                    INDEX idx_user_activity_date (user_id, activity_id, draw_date),
                                    INDEX idx_activity (activity_id),
                                    INDEX idx_draw_time (draw_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户抽奖记录表';

CREATE TABLE t_user_prize_record (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
                                     user_id INT NOT NULL COMMENT '用户ID',
                                     activity_id INT NOT NULL COMMENT '活动ID',
                                     prize_id INT NOT NULL COMMENT '奖品ID',
                                     win_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '中奖时间',
                                     win_date DATE GENERATED ALWAYS AS (DATE(win_time)) STORED COMMENT '中奖日期',
                                     INDEX idx_user_activity_date (user_id, activity_id, win_date),
                                     INDEX idx_activity (activity_id),
                                     INDEX idx_prize (prize_id),
                                     INDEX idx_win_time (win_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户中奖记录表';

