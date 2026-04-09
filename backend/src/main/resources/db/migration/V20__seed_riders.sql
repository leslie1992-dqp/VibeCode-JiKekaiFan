-- 初始化骑手演示数据：绑定 user 表前 3 位用户

INSERT INTO `rider` (`user_id`, `display_name`, `phone`, `vehicle_type`, `rating`, `max_concurrent_tasks`, `status`)
SELECT u.id,
       CONCAT('骑手', u.id),
       u.mobile,
       'E_BIKE',
       4.80,
       3,
       1
FROM `user` u
WHERE u.status = 1
  AND u.id <= 3
  AND NOT EXISTS (SELECT 1 FROM `rider` r WHERE r.user_id = u.id);

INSERT INTO `rider_shift` (`rider_id`, `status`, `current_task_count`, `online_at`)
SELECT r.id, 1, 0, NOW()
FROM `rider` r
WHERE NOT EXISTS (SELECT 1 FROM `rider_shift` rs WHERE rs.rider_id = r.id);
