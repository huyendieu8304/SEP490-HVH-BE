package com.sep490.g28.hvh.be.integration.faceServer;

import com.sep490.g28.hvh.be.integration.faceServer.dto.AuthenticateFaceResponse;
import com.sep490.g28.hvh.be.integration.faceServer.dto.RegisterFaceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * FaceAPI service contract.
 *
 * <p>Defines operations for communication with face api server</p>
 */
public interface FaceAuthClient {

    AuthenticateFaceResponse authenticateFace(MultipartFile file);

    RegisterFaceResponse registerFaceBiometric(String userName, UUID userId, MultipartFile file);
}
