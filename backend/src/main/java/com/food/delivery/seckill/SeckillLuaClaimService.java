package com.food.delivery.seckill;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 使用 Redis + Lua 原子完成：先判重、再读库存、DECR、SADD；整段脚本由 Redis 单线程执行，防超卖与一人一单。
 */
@Service
public class SeckillLuaClaimService {

    /** 与 seckill_claim.lua 返回值一致 */
    public static final long LUA_NOT_INITIALIZED = -1L;
    public static final long LUA_SOLD_OUT = 0L;
    public static final long LUA_OK = 1L;
    public static final long LUA_ALREADY_CLAIMED = 2L;

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> claimScript;

    public SeckillLuaClaimService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(readClasspathUtf8("scripts/seckill_claim.lua"));
        script.setResultType(Long.class);
        this.claimScript = script;
    }

    private static String readClasspathUtf8(String path) {
        ClassPathResource res = new ClassPathResource(path);
        try {
            return StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("load redis script " + path, e);
        }
    }

    /**
     * 若尚无库存 key，则用当前 DB 读到的剩余库存初始化（演示用；生产需预热与对账任务）。
     */
    public void warmStockIfAbsent(long couponId, int stockRemain) {
        String key = SeckillRedisKeys.stockKey(couponId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            return;
        }
        stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(Math.max(0, stockRemain)));
    }

    /**
     * @return LUA_* 常量
     */
    public long tryClaim(long userId, long couponId) {
        List<String> keys = List.of(
                SeckillRedisKeys.stockKey(couponId),
                SeckillRedisKeys.claimedSetKey(couponId)
        );
        Long r = stringRedisTemplate.execute(claimScript, keys, String.valueOf(userId));
        return r == null ? LUA_SOLD_OUT : r;
    }

    /**
     * Kafka 发送失败等场景下回滚 Lua 侧状态，避免长期占库存。
     */
    public void rollbackRedisClaim(long userId, long couponId) {
        stringRedisTemplate.opsForValue().increment(SeckillRedisKeys.stockKey(couponId));
        stringRedisTemplate.opsForSet().remove(SeckillRedisKeys.claimedSetKey(couponId), String.valueOf(userId));
    }
}
