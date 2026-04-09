-- 补充骑手：不限 user.id<=3。仅当「尚无骑手记录」时为前若干名活跃用户建骑手与在线班次，
-- 避免本地注册用户 id 较大时 V20 未插入任何骑手，导致派单永远 NO_RIDER。

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
  AND NOT EXISTS (SELECT 1 FROM `rider` r WHERE r.user_id = u.id)
ORDER BY u.id ASC
LIMIT 10;

INSERT INTO `rider_shift` (`rider_id`, `status`, `current_task_count`, `online_at`)
SELECT r.id, 1, 0, NOW()
FROM `rider` r
WHERE NOT EXISTS (SELECT 1 FROM `rider_shift` rs WHERE rs.rider_id = r.id);
