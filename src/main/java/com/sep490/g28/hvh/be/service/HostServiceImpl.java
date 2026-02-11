package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.dto.host.CreateHostAccountRequest;
import com.sep490.g28.hvh.be.dto.host.CreateMultipleHostAccountRequest;
import com.sep490.g28.hvh.be.entity.Host;
import com.sep490.g28.hvh.be.entity.OrganizationManager;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.HostErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.mail.EmailService;
import com.sep490.g28.hvh.be.repository.HostRepository;
import com.sep490.g28.hvh.be.repository.OrganizationManagerRepository;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.util.RandomStringUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.sep490.g28.hvh.be.util.StringNormalizeUtil.normalizeVietnameseName;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HostServiceImpl implements HostService{
    HostRepository hostRepository;
    OrganizationManagerRepository organizationManagerRepository;
    UserRepository userRepository;

    AuthClient authClient;
    EmailService emailService;

    CurrentUserProvider currentUserProvider;

    @Override
    public String createAccount(CreateMultipleHostAccountRequest request) {
        OrganizationManager organizationManager = organizationManagerRepository.getReferenceById(currentUserProvider.getId());
        List<String> errors = new ArrayList<>();

        for(CreateHostAccountRequest r : request.getRequests()){
            //check whether email used by any account
            //maybe need to check email unique in the same request
            if (userRepository.existsByEmail(r.getEmail())){
                errors.add(r.getEmail() + ": " + HostErrorCode.EMAIL_USED.getMessage());
                continue;
            }

            String defaultPassword = RandomStringUtil.random8AlphaNumeric();
            try {
                //create host account in auth server
                UUID hostId = authClient.createAccount(ERole.HOST, r.getEmail(), defaultPassword, r.getPhone());

                Host host = new Host();
                host.setId(hostId);
                host.setCid(r.getCid());
                host.setEmail(r.getEmail());
                host.setPhone(r.getPhone());
                host.setFullName(normalizeVietnameseName(r.getFullName()));
                host.setAddress(r.getAddress());
                host.setDetailAddress(r.getDetailAddress());
                host.setCreatedBy(organizationManager);
                host.setOrganization(organizationManager.getOrganization());
                //save to db
                hostRepository.save(host);
                log.info("Create host account, id={}", hostId);

                //send mail
                emailService.sendCreateHostAccountEmail(
                        organizationManager.getOrganization().getName(),
                        r.getEmail(),
                        defaultPassword
                        );
            } catch (AppException e) {
                errors.add(r.getEmail() + ": " + e.getMessage());
            }
        }

        if (errors.isEmpty()) {
            return "OK";
        }

        return String.join("\n", errors);
    }
}
