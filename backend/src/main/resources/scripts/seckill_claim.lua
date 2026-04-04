-- 秒杀领券：整段在 Redis 单线程原子执行，防超卖 + 一人一单
-- KEYS[1] = stock key（string 整数）
-- KEYS[2] = claimed set（已成功领取的 userId）
-- ARGV[1] = userId（string）
-- 返回：1=成功；0=库存不足；2=已领过；-1=stock key 不存在

local stockKey = KEYS[1]
local claimedKey = KEYS[2]
local userId = ARGV[1]

if redis.call('EXISTS', stockKey) == 0 then
  return -1
end

if redis.call('SISMEMBER', claimedKey, userId) == 1 then
  return 2
end

local s = tonumber(redis.call('GET', stockKey) or '0')
if s <= 0 then
  return 0
end

redis.call('DECR', stockKey)
local added = redis.call('SADD', claimedKey, userId)
if added == 0 then
  redis.call('INCR', stockKey)
  return 2
end
return 1
