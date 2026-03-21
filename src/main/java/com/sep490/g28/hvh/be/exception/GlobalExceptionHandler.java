package com.sep490.g28.hvh.be.exception;

import com.sep490.g28.hvh.be.dto.ExceptionResponse;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

/**
 * Global exception handler for REST controllers.
 *
 * <p>Handles {@link AppException} explicitly and delegates
 * all other unhandled exceptions to the default mechanism
 * after logging.</p>
 */
@ControllerAdvice
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    /**
     * Handles application-level exceptions.
     *
     * @param e application exception
     * @return standardized error response
     */
    @ExceptionHandler(AppException.class)
    ResponseEntity<ExceptionResponse> appExceptionHandler(AppException e) {
        log.info("Exception is catch by appExceptionHandler, errorCode={} exception={}", e.getResponseMessage(), e.getMessage());
        var response = new ExceptionResponse();
        response.setCode(e.getCode());
        response.setMessage(e.getResponseMessage());
        response.setMoreInfo(Map.of("business", e.getMessage()));
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ExceptionResponse> accessDeniedHandler(AuthorizationDeniedException ex) {
        log.info("AuthorizationDeniedException is catch by accessDeniedHandler");
        AppCommonErrorCode errorCode = AppCommonErrorCode.UNAUTHORIZED;
        var response = new ExceptionResponse();
        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.name());
        response.setMoreInfo(Map.of("auth", errorCode.getMessage()));
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }
    /**
     * Catch-all handler for unexpected exceptions.
     *
     * <p>Logs the exception and rethrows it to avoid
     * silently swallowing errors.</p>
     */
    //Note: This method should place in the final of this class to
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> catchAll(Exception ex) throws Exception {
        log.error("UNHANDLED EXCEPTION", ex);
        throw ex;
    }
}
