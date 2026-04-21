package com.sep490.g28.hvh.be.integration.cache;

import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.util.RandomStringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-based implementation of {@link OtpService}.
 *
 * <p>Features:
 * <ul>
 *   <li>OTP stored in Redis with TTL</li>
 *   <li>Cooldown between OTP generations</li>
 *   <li>Generation rate limit within a sliding window</li>
 *   <li>Maximum verification attempts</li>
 * </ul>
 *
 * <p>OTP lifecycle is fully managed in Redis and
 * cleaned up automatically on success or failure.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisOtpService implements OtpService {

    private static final String TYPE_VOL_REGISTER_PRE = "otp:vol-register:";
    private static final String TYPE_ORG_REGISTER_PRE = "otp:org-register:";
    private static final String TYPE_FORGOT_PASSWORD_PRE = "otp:forgot-password:";
    private static final String TYPE_CHANGE_PHONE_NUMBER_PRE = "otp:change-phone:";
    private static final String OTP_SUF = "otp";
    private static final String ATTEMPT_SUF = "attempt";
    private static final int MAX_ATTEMPT = 3;
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int MAX_GEN = 3;

    private final RedisTemplate<String, String> redisTemplate;


    @Override
    public String getVolAccountRegistrationOtp(String email) {
        String key = TYPE_VOL_REGISTER_PRE + email;
        return generateOtp(key);
    }

    @Override
    public boolean verifyVolAccountRegistrationOtp(String email, String inputOtp) {
        String key = TYPE_VOL_REGISTER_PRE + email;
        return verifyOtp(key, inputOtp);
    }

    @Override
    public String getOrgRegistrationOtp(String email) {
        String key = TYPE_ORG_REGISTER_PRE + email;
        return generateOtp(key);
    }

    @Override
    public boolean verifyOrgRegistrationOtp(String email, String inputOtp) {
        String key = TYPE_ORG_REGISTER_PRE + email;
        return verifyOtp(key, inputOtp);
    }

    @Override
    public String getVerifyForgotPasswordOtp(String email) {
        String key = TYPE_FORGOT_PASSWORD_PRE + email;
        return generateOtp(key);
    }

    @Override
    public boolean verifyVerifyForgotPasswordOtp(String email, String inputOtp) {
        String key = TYPE_FORGOT_PASSWORD_PRE + email;
        return verifyOtp(key, inputOtp);
    }

    @Override
    public String getVerifyChangePhoneNumberOtp(String email) {
        String key = TYPE_CHANGE_PHONE_NUMBER_PRE + email;
        return generateOtp(key);
    }

    @Override
    public boolean verifyVerifyChangePhoneNumberOtp(String email, String inputOtp) {
        String key = TYPE_CHANGE_PHONE_NUMBER_PRE + email;
        return verifyOtp(key, inputOtp);
    }


    /**
     * Generates a new OTP with cooldown and rate limiting.
     *
     * @throws AppException if cooldown is active or generation limit exceeded
     */
    private String generateOtp(String key) {

        String cooldownKey = key + ":cooldown";
        String genCountKey = key + ":gen_count";


        // 1. Cooldown check (atomic)
        Boolean allowed = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", COOLDOWN);

        if (Boolean.FALSE.equals(allowed)) {
            throw new AppException(AppCommonErrorCode.OTP_STILL_COOLDOWN);
        }

        // 2. Window limit
        Long count = redisTemplate.opsForValue().increment(genCountKey);

        if (count != null && count == 1) { //just the first time
            redisTemplate.expire(genCountKey, WINDOW);
        }

        if (count != null && count > MAX_GEN) { //exceed limit
            throw new AppException(AppCommonErrorCode.OTP_TOO_MANY_REQUESTS);
        }

        // 3. Generate otp
        String otp = RandomStringUtil.random6Numberic();

        HashOperations<String, String, String> hash = redisTemplate.opsForHash();
        hash.put(key, OTP_SUF, otp);
        hash.put(key, ATTEMPT_SUF, "0");
        redisTemplate.expire(key, TTL);

        return otp;
    }

    /**
     * Verifies OTP and enforces attempt limits.
     *
     * @throws AppException if OTP is expired, invalid, or attempts exceeded
     */
    private boolean verifyOtp(String key, String inputOtp) {
        HashOperations<String, String, String> hash = redisTemplate.opsForHash();

        String savedOtp = hash.get(key, OTP_SUF);

        //1. check exist
        // cannot get otp from redis -> otp expire
        if (savedOtp == null) throw new AppException(AppCommonErrorCode.OTP_EXPIRED);

        //2. Check attempt
        String attemptStr = hash.get(key, ATTEMPT_SUF);
        int attempt = attemptStr == null ? 0 : Integer.parseInt(attemptStr);
        // user's attempts exceed MAX ATTEMPT
        if (attempt >= MAX_ATTEMPT) {
            redisTemplate.delete(key); //delete
            throw new AppException(AppCommonErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }

        //3. check valid
        //the input otp is not true -> increase attempt
        if (!savedOtp.equals(inputOtp)) {
            hash.increment(key, ATTEMPT_SUF, 1);
            throw new AppException(AppCommonErrorCode.OTP_INVALID);
        }

        //4. correct -> delete from redis
        redisTemplate.delete(key);
        return true;
    }
}
