package com.sep490.g28.hvh.be.util;

import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.SupabaseErrorCode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class AsyncExceptionUtils {

    private AsyncExceptionUtils() {}

    /**
     * Handles a {@link CompletionException} thrown from an async operation,
     * applying a specific rule for Supabase storage errors.
     *
     * <p>Behavior:
     * <ul>
     *     <li>If the root cause is {@link AppException} with error code
     *         {@link SupabaseErrorCode#STORAGE_FILE_NOT_EXISTED}, the exception is ignored.</li>
     *     <li>If the root cause is a {@link RuntimeException}, it is rethrown as-is.</li>
     *     <li>Otherwise, the root cause is wrapped in a {@link RuntimeException} and thrown.</li>
     * </ul>
     *
     * @param ex the {@link CompletionException} thrown by the async operation
     */
    public static void resolveExceptionIgnoreIfFileNotExisted(CompletionException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof AppException ae
                && ae.getCode() == SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED.getCode()) {
            return;
        }

        if (cause instanceof RuntimeException re) {
            throw re;
        }
        throw new RuntimeException(cause);
    }

    /**
     * Resolve a {@link CompletionException} thrown from async operations (e.g. {@link CompletableFuture#join()}),
     * applying domain-specific error handling for Supabase storage.
     *
     * <p>Behavior:
     * <ul>
     *     <li>If the root cause is {@link AppException} with error code
     *         {@link SupabaseErrorCode#STORAGE_FILE_NOT_EXISTED}, return the provided fallback value.</li>
     *     <li>If the root cause is a {@link RuntimeException}, rethrow it as-is.</li>
     *     <li>Otherwise, wrap the root cause into a {@link RuntimeException} and throw.</li>
     * </ul>
     *
     * @param ex        the {@link CompletionException} thrown by async execution
     * @param fallback  the fallback value to return when file does not exist
     * @param <T>       the return type
     * @return fallback value if file not existed, otherwise never returns (exception is thrown)
     */
    public static <T> T resolveExceptionReturnFallbackIfFileNotExisted(CompletionException ex, T fallback) {
        Throwable cause = ex.getCause();

        if (cause instanceof AppException ae
                && ae.getCode() == SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED.getCode()) {
            return fallback;
        }

        if (cause instanceof RuntimeException re) {
            throw re;
        }

        throw new RuntimeException(cause);
    }
}
