package com.sep490.g28.hvh.be.auth;

import com.sep490.g28.hvh.be.constant.ERole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.UUID;

/**
 * Provides access to the {@link CurrentUser} extracted from JWT claims.
 *
 * <p>This component is request-scoped and represents the user
 * who is making the current HTTP request.</p>
 *
 * <p>The {@link CurrentUser} object is expected to be populated
 * during authentication and stored in {@link Authentication#getDetails()}.</p>
 */
@Component
@RequestScope
@RequiredArgsConstructor
public class CurrentUserProvider {

    /**
     * Retrieves the {@link CurrentUser} from the Spring Security context.
     *
     * @return the current authenticated user
     */
    private CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CurrentUser) auth.getDetails();
    }

    /**
     * @return the unique identifier of the current user
     */
    public UUID getId() {
        return get().id();
    }

    /**
     * @return the email of the current user
     */
    public String getEmail() {
        return get().email();
    }

    /**
     * @return the role of the current user
     */
    public ERole getRoleName() {
        return get().roleName();
    }
}
