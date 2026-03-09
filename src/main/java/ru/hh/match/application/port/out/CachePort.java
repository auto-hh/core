package ru.hh.match.application.port.out;

import java.util.Optional;

public interface CachePort {

    void set(String key, String value, long ttlSeconds);

    Optional<String> get(String key);

    boolean exists(String key);

    void delete(String key);
}
