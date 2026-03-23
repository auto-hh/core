package ru.hh.match.infrastructure.adapter.redis;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ru.hh.match.application.port.out.SessionPort;

@Component
public class RedisSessionAdapter implements SessionPort {

    private final StringRedisTemplate redisTemplate;

    public RedisSessionAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<String> getAccessToken(UUID sessionId) {
        String token = redisTemplate.opsForValue().get("access_token_" + sessionId);
        return Optional.ofNullable(token);
    }

    @Override
    public void deleteAccessToken(UUID sessionId) {
        redisTemplate.delete("access_token_" + sessionId);
    }
}
