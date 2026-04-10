package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.organization.request.RegisterOrganizationRequest;
import com.sep490.g28.hvh.be.dto.organization.response.RegisterOrganizationResponse;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.service.EventService;
import com.sep490.g28.hvh.be.service.EventSessionService;
import com.sep490.g28.hvh.be.service.OrganizationService;
import com.sep490.g28.hvh.be.service.impl.CertificateServiceImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/test")
@Validated
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TestController {

//    @PostMapping(
//            value = "/register",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
//    )
//    public ResponseEntity<String> registerVolunteer(
//            @Valid @ModelAttribute RegisterVolunteerAccountRequest request
//    ) {
//        return ResponseEntity.ok("OK");
//    }

    // test ConstraintViolationException (method-level)
    @GetMapping("/phone")
    public ResponseEntity<String> testPhone(
            @Pattern(
                    regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$",
                    message = "INVALID_PHONE"
            )
            @RequestParam String phone
    ) {
        return ResponseEntity.ok("OK");
    }

    private final CurrentUserProvider currentUserProvider;
//    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'VOL')")
//    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {

        log.info("test logging");
        return ResponseEntity.ok(Map.of(
                "userId", currentUserProvider.getId(),
                "email", currentUserProvider.getEmail(),
                "role", currentUserProvider.getRoleName()
        ));
    }

//    private final EmailSenderService emailSenderService;
//    @GetMapping("/mail")
//    public ResponseEntity<?> mail() throws MessagingException {
//
//        String subject = "Test mail";
//        String html = "<h1>Mail OK</h1><p>This is a test</p>";
//
//        emailSenderService.sendEmail("huyendieu8304@gmail.com", subject, html);
//
//        return ResponseEntity.ok("OK");
//    }

//    private final EmailService emailService;
//    @GetMapping("/mail")
//    public ResponseEntity<String> testMail(@RequestParam String email) {
//
//        emailService.sendApproveRegisterVolAccountEmail(email);
//        return ResponseEntity.ok("OK");
//
//    }

    private final OtpService otpService;

    @GetMapping("/otp")
    public ResponseEntity<String> getOtp() {
        log.info("test logging without authenticated");

        return ResponseEntity.ok(otpService.getVolAccountRegistrationOtp("huyendieu8304@gmail.com"));
    }

    @GetMapping("/otp-verify")
    public ResponseEntity<String> testOtp(@RequestParam String otp) {
        if (otpService.verifyVolAccountRegistrationOtp("huyendieu8304@gmail.com", otp)){
        return ResponseEntity.ok("OK");
        }
        return ResponseEntity.ok("oh nooooo");
    }

    OrganizationService organizationService;

    @PostMapping("/register-org")
    public ResponseEntity<RegisterOrganizationResponse> registerOrganization(
            @Valid @RequestBody RegisterOrganizationRequest request
    ) {
        return ResponseEntity.ok(organizationService.registerOrganization(request));
    }

    CertificateServiceImpl certificateServiceImpl;

    VolunteerRepository volunteerRepository;
    EventRepository eventRepository;
    EventService eventService;

    @PostMapping("/certificate")
    public ResponseEntity<String> generate (
            @RequestParam UUID volId,
            @RequestParam UUID eventId
    ){

        Volunteer volunteer = volunteerRepository.findById(volId).isPresent() ? volunteerRepository.findById(volId).get() : null;
        Event event = eventRepository.findById(eventId).isPresent() ? eventRepository.findById(eventId).get() : null;

        certificateServiceImpl.generateCertificate(volunteer, event);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/complete-event")
    public ResponseEntity<Void> completeEvent(){
        eventService.completeEvents();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/end-recruitment")
    public ResponseEntity<Void> endRecruitment(){
        eventService.endRecruitment();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/start-event")
    public ResponseEntity<Void> startEvents(){
        eventService.startEvents();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/end-event")
    public ResponseEntity<Void> endEvents(){
        eventService.endEvents();
        return ResponseEntity.ok().build();
    }


    @PostMapping("/calc-org-rating")
    public ResponseEntity<Void> calculateOrganizationsAvgRating(){
        organizationService.calculateOrganizationsAvgRating();
        return ResponseEntity.ok().build();
    }

    EventSessionService eventSessionService;
    @PostMapping("/create-check-in-code")
    public ResponseEntity<Void> createCheckInCode(){
        eventSessionService.createCheckInCode();
        return ResponseEntity.ok().build();
    }

}
