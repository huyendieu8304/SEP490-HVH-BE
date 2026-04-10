package com.sep490.g28.hvh.be.integration.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for file storage operations.
 * <p>
 * Hides storage provider implementation (Supabase, S3, etc).
 * Used by application layer to generate upload URLs for clients.
 * </p>
 */
public interface StorageService {
    /**
     * Generate a signed upload URL for client-side file upload.
     * <p>
     * Client uploads file directly to Supabase Storage.
     * Backend does not handle file content.
     * </p>
     * @param path             file path inside storage bucket
     * @return signed upload URL
     */
    String getUploadUrl(String path);

    /**
     * Generate a signed upload URL for client-side file upload in async manner.
     * <p>
     * Client uploads file directly to Supabase Storage.
     * Backend does not handle file content.
     * </p>
     * @param path             file path inside storage bucket
     * @return signed upload URL
     */
    CompletableFuture<String> getUploadUrlAsync(String path);

    /**
     * Delete a file in storage.
     *
     * @param path             file path inside storage bucket
     */
    void deleteFile(String path);

    /**
     * Delete a file in storage in async manner.
     *
     * @param path             file path inside storage bucket
     */
    CompletableFuture<Void> deleteFileAsync(String path);

    /**
     * Generate a signed URL for client-side to view it.
     *
     * @param path             file path inside storage bucket
     * @return signed URL
     */
    String getSignedUrl(String path);

    /**
     * Generate a signed URL for client-side to view it in Async manner.
     *
     * @param path             file path inside storage bucket
     * @return signed URL
     */
    CompletableFuture<String> getSignedUrlAsync(String path);

    /**
     * Upload file directly to Supabase Storage.
     * <p>
     * Intended for server-side upload only.
     * </p>
     *
     * @param file multipart file
     * @param path destination path in bucket
     */
    void upload(MultipartFile file, String path);

    void upload(byte[] fileBytes, String path);
}
