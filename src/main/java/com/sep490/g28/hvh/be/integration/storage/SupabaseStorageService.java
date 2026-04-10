package com.sep490.g28.hvh.be.integration.storage;

import com.sep490.g28.hvh.be.config.SupabaseProperties;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.SupabaseException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.SupabaseErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Supabase-based implementation of {@link StorageService}.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Upload files directly to Supabase Storage (server-side)</li>
 *   <li>Generate signed URLs for upload/view</li>
 * </ul>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>Uses Supabase service role key</li>
 *   <li>All paths are scoped to a single bucket</li>
 * </ul>
 */
@Slf4j
@Service
public class SupabaseStorageService implements StorageService {
    private final RestTemplate restTemplate;
    private final SupabaseProperties supabaseProperties;
    private final Executor taskExecutor;

    public SupabaseStorageService(
            @Qualifier("supabaseRestTemplate") RestTemplate restTemplate,
            SupabaseProperties config, Executor taskExecutor
    ) {
        this.restTemplate = restTemplate;
        this.supabaseProperties = config;
        this.taskExecutor = taskExecutor;
    }

    /**
     * Generate signed upload URL for client-side upload.
     * By default, the url would expire in 10 minutes
     *
     * @param path             file path in bucket
     * @return signed upload URL
     */
    @Override
    public String getUploadUrl(String path) {
        String url = supabaseProperties.getUrl()
                + "/storage/v1/object/upload/sign/"
                + supabaseProperties.getBucket()
                + "/" + path;

        //expired in 1 hour
        Map<String, Object> body = Map.of(
                "expiresIn", 600
        );

        try {
            ResponseEntity<Map> res = restTemplate.postForEntity(url, body, Map.class);
            log.info("Get upload url for file with path: {}", path);
            return (String) Objects.requireNonNull(res.getBody()).get("url");
        } catch (Exception e) {
            if (e instanceof SupabaseException se) {
                int status = se.getStatus();
                if (status == 500) {
                    throw new AppException(SupabaseErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
            throw new AppException(SupabaseErrorCode.STORAGE_GET_UPLOAD_URL_FAIL);
        }
    }

    @Override
    public CompletableFuture<String> getUploadUrlAsync(String path) {
//        return CompletableFuture.completedFuture(getUploadUrl(path));
        return CompletableFuture.supplyAsync(() -> getUploadUrl(path), taskExecutor);
    }

    @Override
    public void deleteFile(String path) {
        String url = supabaseProperties.getUrl()
                + "/storage/v1/object/"
                + supabaseProperties.getBucket()
                + "/" + path;

        try {
            restTemplate.delete(url);
            log.info("Delete file in path: {}", path);
        } catch (Exception e) {
            if (e instanceof SupabaseException se) {
                int status = se.getStatus();
                if (status == 400) {
                    throw new AppException(SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED);
                } else if (status == 500) {
                    throw new AppException(SupabaseErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
            throw new AppException(SupabaseErrorCode.STORAGE_DELETE_FILE_FAIL);
        }
    }

    @Override
    public CompletableFuture<Void> deleteFileAsync(String path) {
        return CompletableFuture.runAsync(() -> deleteFile(path), taskExecutor);
    }

    /**
     * Create a signed URL for viewing/downloading a file.
     *
     * @param path file path in bucket
     * @return signed URL (default 1 hour expiry)
     */
    @Override
    public String getSignedUrl(String path) {
        String url = supabaseProperties.getUrl()
                + "/storage/v1/object/sign/"
                + supabaseProperties.getBucket()
                + "/" + path;
        //expired in 1 hour
        Map<String, Object> body = Map.of(
                "expiresIn", 3600
        );

        try {
            ResponseEntity<Map> res =
                    restTemplate.postForEntity(url, body, Map.class);
            log.info("Get signed url for file in path: {}", path);
            return (String) Objects.requireNonNull(res.getBody()).get("signedURL");

        } catch (Exception e) {
            if (e instanceof SupabaseException se) {
                int status = se.getStatus();
                if (status == 400) {
                    throw new AppException(SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED);
                } else if (status == 500) {
                    throw new AppException(SupabaseErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
            throw new AppException(SupabaseErrorCode.STORAGE_GET_SIGNED_URL_FAIL);
        }
    }

    @Override
    public CompletableFuture<String> getSignedUrlAsync(String path) {
        return CompletableFuture.supplyAsync(() -> getSignedUrl(path), taskExecutor);
    }

    @Override
    public void upload(MultipartFile file, String path) {
        String url = supabaseProperties.getUrl()
                + "/storage/v1/object/"
                + supabaseProperties.getBucket()
                + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(file.getSize());

        try {
            InputStreamResource resource = new InputStreamResource(file.getInputStream()) {
                @Override
                public long contentLength() throws IOException {
                    return file.getSize();
                }
            };
            HttpEntity<Resource> request =
                    new HttpEntity<>(resource, headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    Void.class
            );
            log.info("Upload file to path: {}", path);
        } catch (IOException e) {
            log.error("Error occur while upload file: {}",e.getMessage());
        } catch (Exception e){
            if (e instanceof SupabaseException se) {
                int status = se.getStatus();
                if (status == 500) {
                    throw new AppException(SupabaseErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
            throw new AppException(SupabaseErrorCode.STORAGE_UPLOAD_FAIL);
        }
    }

    @Override
    public void upload(byte[] fileBytes, String path) {
        String url = supabaseProperties.getUrl()
                + "/storage/v1/object/"
                + supabaseProperties.getBucket()
                + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(fileBytes.length);

        HttpEntity<byte[]> request = new HttpEntity<>(fileBytes, headers);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    Void.class
            );
            log.info("Upload PDF to path: {}", path);

        } catch (Exception e) {
            if (e instanceof SupabaseException se) {
                if (se.getStatus() == 500) {
                    throw new AppException(SupabaseErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
            throw new AppException(SupabaseErrorCode.STORAGE_UPLOAD_FAIL);
        }
    }
}