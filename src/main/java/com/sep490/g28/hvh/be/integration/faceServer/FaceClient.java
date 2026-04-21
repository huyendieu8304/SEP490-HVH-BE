package com.sep490.g28.hvh.be.integration.faceServer;

import com.sep490.g28.hvh.be.integration.faceServer.dto.FaceAuthenticationResponse;
import com.sep490.g28.hvh.be.integration.faceServer.dto.FaceRegisterResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * FaceAPI service contract.
 *
 * <p>Defines operations for communication with face api server</p>
 */
public interface FaceClient {

    FaceAuthenticationResponse faceAuthentication(MultipartFile file);

    FaceRegisterResponse faceRegister(String userName, UUID userId, MultipartFile file);
}
