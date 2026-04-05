package com.sep490.g28.hvh.be.integration.authServer;

import com.sep490.g28.hvh.be.config.SupabaseProperties;
import com.sep490.g28.hvh.be.constant.EAccountStatus;
import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.integration.authServer.dto.CreateUserRequest;
import com.sep490.g28.hvh.be.integration.authServer.dto.UserResponse;
import com.sep490.g28.hvh.be.entity.User;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.SupabaseException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.SupabaseErrorCode;
import com.sep490.g28.hvh.be.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Supabase-based implementation of {@link AuthClient}.
 *
 * <p>Uses Supabase Admin REST API to manage user accounts.</p>
 *
 * <p>Notes:
 * <ul>
 *   <li>Requires Supabase service role key</li>
 *   <li>Performs server-to-server calls via {@link RestTemplate}</li>
 *   <li>Stores role and phone number in {@code app_metadata}</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class SupabaseAuthClient implements AuthClient {
    private final RestTemplate restTemplate;
    private final SupabaseProperties supabaseProperties;
    private final UserRepository userRepository;

    public SupabaseAuthClient(
            @Qualifier("supabaseRestTemplate") RestTemplate restTemplate,
            SupabaseProperties config,
            UserRepository userRepository
    ) {
        this.restTemplate = restTemplate;
        this.supabaseProperties = config;
        this.userRepository = userRepository;
    }

    /**
     * Creates a Supabase user using Admin API.
     *
     * <p>The user is created as email-verified by default
     * and includes custom {@code app_metadata}.</p>
     *
     * @throws AppException if Supabase returns an error
     */
    @Override
    public UUID createAccount(ERole role, String email, String password, String phone) {
        Map<String, Object> appMetadata = Map.of(
                "role", role.name(),
                "phone", phone
        );
        CreateUserRequest request = new CreateUserRequest(
                email,
                password,
                true,
                appMetadata
        );

        String url = supabaseProperties.getUrl() + "/auth/v1/admin/users";

        HttpEntity<CreateUserRequest> httpEntity =
                new HttpEntity<>(request);

        try {
            ResponseEntity<UserResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpEntity,
                    UserResponse.class
            );
            UUID id = Objects.requireNonNull(responseEntity.getBody()).id();
            log.info("Create user id={}", id);
            //save email to table user in db
            User user = new User();
            user.setId(id);
            user.setEmail(email);
            user.setStatus(EAccountStatus.ACTIVE);
            userRepository.save(user);
            return id;
        } catch (Exception e) {
            if (e instanceof SupabaseException se){
                int status = se.getStatus();
                if (status == 500) {
                    throw new AppException(SupabaseErrorCode.INTERNAL_SERVER_ERROR);
                } else if (status == 422) {
                    throw new AppException(SupabaseErrorCode.AUTH_EMAIL_USED);
                }
            }
            throw new AppException(SupabaseErrorCode.AUTH_CREATE_ACCOUNT_FAIL);
        }

    }

    @Override
    public void changePassword(UUID accountId, String newPassword) {
        String url = supabaseProperties.getUrl()
                + "/auth/v1/admin/users/" + accountId;

        Map<String, Object> body = Map.of(
                "password", newPassword
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body);

        try {
            ResponseEntity<UserResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    UserResponse.class // THIS COULD BE VOID
            );
            log.info(Objects.requireNonNull(responseEntity.getBody()).toString());
        } catch (Exception e) {
            if (e instanceof SupabaseException se){
                int status = se.getStatus();
                if (status == 500) {
                    throw new AppException(SupabaseErrorCode.INTERNAL_SERVER_ERROR);
                } else if (status == 404) {
                    throw new AppException(SupabaseErrorCode.AUTH_ACCOUNT_NOT_EXISTED);
                }
            }
            throw new AppException(SupabaseErrorCode.AUTH_CHANGE_PASSWORD_FAIL);
        }
    }

    @Override
    public UserResponse getAccountInfo(UUID accountId) {
        String url = supabaseProperties.getUrl() + "/auth/v1/admin/users/" + accountId;

        try {
            ResponseEntity<UserResponse> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    UserResponse.class
            );
            return responseEntity.getBody();
        } catch (Exception e) {
            if (e instanceof SupabaseException se){
                int status = se.getStatus();
                if (status == 500) {
                    throw new AppException(SupabaseErrorCode.INTERNAL_SERVER_ERROR);
                } else if (status == 404) {
                    throw new AppException(SupabaseErrorCode.AUTH_ACCOUNT_NOT_EXISTED);
                }
            }
            throw new AppException(SupabaseErrorCode.AUTH_CHANGE_PASSWORD_FAIL);
        }

    }

    @Override
    public boolean isAccountActive(UUID accountId) {
        UserResponse user = getAccountInfo(accountId);
        //not been banned or already end banned
        return user.banned_until() == null
                || user.banned_until().isBefore(OffsetDateTime.now());
    }


}
