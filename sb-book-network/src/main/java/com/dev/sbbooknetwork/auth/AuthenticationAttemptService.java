package com.dev.sbbooknetwork.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class AuthenticationAttemptService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationAttemptService.class);

    private static final int MAXIMUM_NUMBER_OF_ATTEMPTS = 5;
    private final LoadingCache<String, Integer> authAttemptCache;

    public AuthenticationAttemptService() {
        authAttemptCache = CacheBuilder.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    public void evictUserFromAuthAttemptCache(String email) {
        authAttemptCache.invalidate(email);
        logger.debug("User {} evicted from cache", email);
    }

    public void addUserToAuthAttemptCache(String email) {
        try {
            int attempts = authAttemptCache.get(email, () -> 0);
            attempts = Math.min(attempts + 1, MAXIMUM_NUMBER_OF_ATTEMPTS); // Tăng lên và giới hạn không vượt quá MAXIMUM_NUMBER_OF_ATTEMPTS
            authAttemptCache.put(email, attempts);
            logger.debug("User {} attempts incremented to {}", email, attempts);
        } catch (ExecutionException e) {
            logger.error("Error adding user {} to attempt cache: {}", email, e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean hasExceededMaxAttempts(String email) {
        try {
            int attempts = authAttemptCache.get(email);
            logger.debug("User {} has {} attempts", email, attempts);
            return attempts >= MAXIMUM_NUMBER_OF_ATTEMPTS;
        } catch (ExecutionException e) {
            logger.error("Error retrieving attempts for user {}: {}", email, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public int getLoginAttempts(String email) {
        try {
            int attempts = authAttemptCache.get(email, () -> 0);
            logger.debug("User {} login attempts updated to {}", email, attempts);
            return attempts;
        } catch (ExecutionException e) {
            logger.error("Error retrieving attempts for user {}: {}", email, e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    public void resetAttempts(String email) {
        authAttemptCache.put(email, 0);
        logger.debug("User {} login attempts reset to 0", email);
    }
}
