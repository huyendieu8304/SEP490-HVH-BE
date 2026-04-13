package com.sep490.g28.hvh.be.integration.faceServer;


import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.FaceApiErrorCode;
import com.sep490.g28.hvh.be.integration.faceServer.dto.FaceAuthenticationResponse;
import com.sep490.g28.hvh.be.integration.faceServer.dto.FaceRegisterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

/**
 * FaceAPI-based client implementation of {@link FaceClient}.
 *
 * <p>Uses FaceAPI to manage verification by face.</p>
 *
 */
@Slf4j
@Service
public class FaceAuthClient implements FaceClient{

    @Value("${face-authen.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public FaceAuthClient(
            @Qualifier("faceapiRestTemplate") RestTemplate restTemplate
    ) {
        this.restTemplate = restTemplate;
    }

    @Override
    public FaceAuthenticationResponse faceAuthentication(MultipartFile file) {
        String url = baseUrl + "/auth/video";

        ResponseEntity<FaceAuthenticationResponse> response = null;

        try {
            File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile));

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body);

            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    FaceAuthenticationResponse.class
            );
        } catch (Exception e) {
            log.error("Error occur while authenticate face: {}",e.getMessage());
            throw new AppException(FaceApiErrorCode.VALIDATION_FAIL);
        }

        log.info("Response For Face Authentication: {}", response);

        return response.getBody();
    }

    @Override
    public FaceRegisterResponse faceRegister(String userName, UUID userId, MultipartFile file) {
        String url = baseUrl + "/enroll_single/" + userName + "/" + userId;
        ResponseEntity<FaceRegisterResponse> response = null;

        try {
            File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile));

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body);

            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    FaceRegisterResponse.class
            );
        } catch (Exception e) {
            log.error("Error occur while register face: {}",e.getMessage());
            throw new AppException(FaceApiErrorCode.VALIDATION_FAIL);
        }

        log.info("Response For Face Register: {}", response);

        return response.getBody();
    }


}
