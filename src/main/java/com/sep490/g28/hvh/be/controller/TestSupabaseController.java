package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.integration.authServer.dto.UserResponse;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.faceServer.FaceAuthClient;
import com.sep490.g28.hvh.be.integration.faceServer.dto.AuthenticateFaceResponse;
import com.sep490.g28.hvh.be.integration.faceServer.dto.RegisterFaceResponse;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@RestController
@RequestMapping("/test-sb")
@RequiredArgsConstructor
public class TestSupabaseController {

    private final AuthClient authClient;
    private final FaceAuthClient faceAuthClient;
    private final StorageService storageService;

//    public TestSupabaseController(SupabaseAuthClient authClient, SupabaseStorageService storageService) {
//        this.authClient = authClient;
//        this.storageService = storageService;
//    }

    // test upload file
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("path") String path
    ){

        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".")))
                .orElse("");

        String savePath = path + ext;

        storageService.upload(file, savePath);

        return ResponseEntity.ok(
                "upload sucess "
        );

    }

    @PostMapping("/delete")
    public ResponseEntity<String> delete(
            @RequestParam("path") String path
    ){

        storageService.deleteFile(path);

        return ResponseEntity.ok(
                "delete sucess "
        );
    }

    @PostMapping("/delete-multiple")
    public ResponseEntity<String> deleteMultiple(
            @RequestParam("path") String path1,
            @RequestParam("path2") String path2
    ){

        CompletableFuture<Void> delete1 = storageService.deleteFileAsync(path1);
        CompletableFuture<Void> delete2 = storageService.deleteFileAsync(path2);

        try {
            CompletableFuture.allOf(delete1, delete2).join();
        } catch (CompletionException e) {
            throw (RuntimeException) e.getCause();
        }

        return ResponseEntity.ok(
                "delete multiple sucess "
        );
    }

//     test create signed url
    @GetMapping("/signed-url")
    public ResponseEntity<Map<String, String>> getSignedUrl(
            @RequestParam("path") String path
    ) {
        String signedUrl = storageService.getSignedUrl(path);

        return ResponseEntity.ok(
                Map.of("signedURL", signedUrl)
        );
    }

    private final UserRepository userRepository;
    @GetMapping("/create-account")
    public ResponseEntity<String> testCreateVolAccount(@RequestParam String email, @RequestParam ERole role) {
        if (userRepository.existsByEmail(email)){
            return ResponseEntity.ok("oh oh emddaxd dc su dung");
        }
        authClient.createAccount(role, email, "12345678", "0123456789");
        return ResponseEntity.ok("OK");
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestParam UUID accountId,
            @RequestParam String newPassword
    ) {
//        String newPassword = RandomStringUtil.random8AlphaNumeric();
        authClient.changePassword(accountId, newPassword);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/acount-info")
    public ResponseEntity<UserResponse> getAccountInfo(
            @RequestParam UUID accountId
    ){
        return ResponseEntity.ok(authClient.getAccountInfo(accountId));
    }


    @GetMapping("/check-password")
    public ResponseEntity<String> getAccountInfo(
            @RequestParam String email,
            @RequestParam String password
    ){
        if (authClient.checkOldPassword(email, password)){
            return ResponseEntity.ok("OK");
        } else
            return ResponseEntity.ok("ehhhhh");
    }

    @PostMapping("/face-authentication")
    public ResponseEntity<AuthenticateFaceResponse> upload(@RequestPart("file") MultipartFile file) {
        AuthenticateFaceResponse result = faceAuthClient.authenticateFace(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/face-register/{userName}/{userId}")
    public ResponseEntity<RegisterFaceResponse> register(
            @PathVariable("userName")
            String userName,

            @PathVariable("userId")
            UUID userId,

            @RequestPart("file")
            MultipartFile file) {
        RegisterFaceResponse result = faceAuthClient.registerFaceBiometric(userName, userId, file);
        return ResponseEntity.ok(result);
    }

}
