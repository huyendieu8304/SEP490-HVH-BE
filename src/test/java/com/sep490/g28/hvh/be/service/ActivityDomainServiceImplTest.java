package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.activityDomain.request.*;
import com.sep490.g28.hvh.be.dto.activityDomain.response.ActivityDomainDetailsResponse;
import com.sep490.g28.hvh.be.entity.ActivityDomain;
import com.sep490.g28.hvh.be.entity.ActivitySubDomain;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ActivityDomainErrorCode;
import com.sep490.g28.hvh.be.repository.ActivityDomainRepository;
import com.sep490.g28.hvh.be.repository.ActivitySubDomainRepository;
import com.sep490.g28.hvh.be.service.impl.ActivityDomainServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;



@ExtendWith(MockitoExtension.class)
public class ActivityDomainServiceImplTest {

    @Mock
    ActivityDomainRepository activityDomainRepository;

    @Mock
    ActivitySubDomainRepository activitySubDomainRepository;

    ActivityDomainServiceImpl activityDomainService;

    Short domainId = 1;
    ActivityDomain activityDomain;
    ActivitySubDomain activitySubDomain;

    @BeforeEach
    void setup() {
        activityDomainService = new ActivityDomainServiceImpl(
                activityDomainRepository,
                activitySubDomainRepository
        );
    }

    private CreateActivityDomainRequest validCreateRequest() {
        CreateActivityDomainRequest request = new CreateActivityDomainRequest();
        request.setName("Domain A");
        request.setSpecialSessionMaxTime(Short.valueOf("8"));
        request.setActivitySubDomain(List.of("Subdomain 1", "Subdomain 2"));
        return request;
    }

    private CreateActivityDomainRequest createRequestNoSubDomain() {
        CreateActivityDomainRequest request = new CreateActivityDomainRequest();
        request.setName("Domain B");
        request.setSpecialSessionMaxTime(Short.valueOf("8"));
        request.setActivitySubDomain(null);
        return request;
    }

    private UpdateActivityDomainRequest validUpdateRequest() {
        UpdateActivityDomainRequest request = new UpdateActivityDomainRequest();
        request.setName("Updated Domain A");
        request.setSpecialSessionMaxTime(Short.valueOf("8"));
        return request;
    }

    private ChangeActivityDomainVisibilityRequest validChangeVisibilityRequest() {
        ChangeActivityDomainVisibilityRequest request = new ChangeActivityDomainVisibilityRequest();
        request.setIsVisible(true);
        return request;
    }

    private ChangeActivitySubDomainVisibilityRequest validChangeSubDomainVisibilityRequest() {
        ChangeActivitySubDomainVisibilityRequest request = new ChangeActivitySubDomainVisibilityRequest();
        request.setIsVisible(true);
        return request;
    }

    private ActivityDomain validActivityDomain() {
        ActivityDomain domain = new ActivityDomain();
        domain.setId(domainId);
        domain.setName("Domain A");
        domain.setActive(true);
        return domain;
    }

    private ActivitySubDomain validActivitySubDomain() {
        ActivitySubDomain subDomain = new ActivitySubDomain();
        subDomain.setId((short) 1);
        subDomain.setName("Subdomain 1");
        subDomain.setActive(true);
        return subDomain;
    }

    // ==== createActivityDomain ===================================
    // ===== TC1 =====
    @Test
    void createActivityDomain_success_with_subdomains() {
        CreateActivityDomainRequest request = validCreateRequest();

        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(activitySubDomainRepository.existsByNameIgnoreCase("Subdomain 1")).thenReturn(false);
        when(activitySubDomainRepository.existsByNameIgnoreCase("Subdomain 2")).thenReturn(false);

        activityDomainService.createActivityDomain(request);

        verify(activityDomainRepository).save(any(ActivityDomain.class));
        verify(activitySubDomainRepository, times(2)).save(any(ActivitySubDomain.class));
    }

    // ===== TC2 =====
    @Test
    void createActivityDomain_success_no_subdomains() {
        CreateActivityDomainRequest request = createRequestNoSubDomain();

        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);

        activityDomainService.createActivityDomain(request);

        verify(activityDomainRepository).save(any(ActivityDomain.class));
        verify(activitySubDomainRepository, never()).save(any());
    }

    // ===== TC3 =====
    @Test
    void createActivityDomain_fail_domain_name_existed() {
        CreateActivityDomainRequest request = validCreateRequest();
        request.setName("Domain C");
        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> activityDomainService.createActivityDomain(request)
        );
        assertEquals(ActivityDomainErrorCode.DOMAIN_NAME_EXISTED.getCode(), ex.getCode());

        verify(activityDomainRepository, never()).save(any());
    }

    // ===== TC4 =====
    @Test
    void createActivityDomain_fail_subdomain_name_existed() {
        CreateActivityDomainRequest request = validCreateRequest();
        request.setActivitySubDomain(List.of("Subdomain A"));
        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(activitySubDomainRepository.existsByNameIgnoreCase("Subdomain A")).thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> activityDomainService.createActivityDomain(request)
        );
        assertEquals(ActivityDomainErrorCode.SUBDOMAIN_NAME_EXISTED.getCode(), ex.getCode());
    }

    // ==== updateActivityDomain ===================================
    // ===== TC1 =====
    @Test
    void updateActivityDomain_success_no_subdomain_changes() {
        UpdateActivityDomainRequest request = validUpdateRequest();
        activityDomain = validActivityDomain();

        when(activityDomainRepository.findById(domainId)).thenReturn(Optional.of(activityDomain));
        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);

        String result = activityDomainService.updateActivityDomain(domainId, request);

        assertEquals("", result);
        verify(activityDomainRepository).save(activityDomain);
        verify(activitySubDomainRepository, never()).save(any());
        verify(activitySubDomainRepository, never()).delete(any());
    }

    // ===== TC2 =====
    @Test
    void updateActivityDomain_success_add_subdomain() {
        UpdateActivityDomainRequest request = validUpdateRequest();
        activityDomain = validActivityDomain();

        UpdateActivitySubDomainRequest addRequest = new UpdateActivitySubDomainRequest();
        addRequest.setAction("ADD");
        addRequest.setName("New Subdomain");

        request.setActivitySubDomainUpdateRequests(List.of(addRequest));

        when(activityDomainRepository.findById(domainId)).thenReturn(Optional.of(activityDomain));
        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(activitySubDomainRepository.existsByNameIgnoreCase("New Subdomain")).thenReturn(false);

        String result = activityDomainService.updateActivityDomain(domainId, request);

        assertEquals("", result);
        verify(activitySubDomainRepository).save(any(ActivitySubDomain.class));
    }

    // ===== TC3 =====
    @Test
    void updateActivityDomain_success_edit_subdomain() {
        UpdateActivityDomainRequest request = validUpdateRequest();
        activityDomain = validActivityDomain();

        UpdateActivitySubDomainRequest editRequest = new UpdateActivitySubDomainRequest();
        editRequest.setId((short) 1);
        editRequest.setAction("EDIT");
        editRequest.setName("Updated Subdomain");

        ActivitySubDomain subDomain = new ActivitySubDomain();
        subDomain.setId((short) 1);

        request.setActivitySubDomainUpdateRequests(List.of(editRequest));

        when(activityDomainRepository.findById(domainId)).thenReturn(Optional.of(activityDomain));
        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(activitySubDomainRepository.findById((short) 1)).thenReturn(Optional.of(subDomain));
        when(activitySubDomainRepository.existsByNameIgnoreCase("Updated Subdomain")).thenReturn(false);

        String result = activityDomainService.updateActivityDomain(domainId, request);

        assertEquals("", result);
        verify(activitySubDomainRepository).save(subDomain);
    }

    // ===== TC4 =====
    @Test
    void updateActivityDomain_success_delete_subdomain() {
        UpdateActivityDomainRequest request = validUpdateRequest();
        activityDomain = validActivityDomain();

        UpdateActivitySubDomainRequest deleteRequest = new UpdateActivitySubDomainRequest();
        deleteRequest.setId((short) 1);
        deleteRequest.setAction("DELETE");

        ActivitySubDomain subDomain = new ActivitySubDomain();
        subDomain.setId((short) 1);

        request.setActivitySubDomainUpdateRequests(List.of(deleteRequest));

        when(activityDomainRepository.findById(domainId)).thenReturn(Optional.of(activityDomain));
        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(activitySubDomainRepository.findById((short) 1)).thenReturn(Optional.of(subDomain));

        String result = activityDomainService.updateActivityDomain(domainId, request);

        assertEquals("", result);
        verify(activitySubDomainRepository).delete(subDomain);
    }

    // ===== TC5 =====
    @Test
    void updateActivityDomain_fail_domain_not_existed() {
        UpdateActivityDomainRequest request = validUpdateRequest();
        when(activityDomainRepository.findById(Short.valueOf("2"))).thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> activityDomainService.updateActivityDomain(Short.valueOf("2"), request)
        );
        assertEquals(ActivityDomainErrorCode.DOMAIN_NOT_EXISTED.getCode(), ex.getCode());
    }

    // ===== TC6 =====
    @Test
    void updateActivityDomain_fail_domain_name_existed() {
        UpdateActivityDomainRequest request = validUpdateRequest();
        request.setName("Updated Domain B");
        activityDomain = validActivityDomain();

        when(activityDomainRepository.findById(domainId)).thenReturn(Optional.of(activityDomain));
        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> activityDomainService.updateActivityDomain(domainId, request)
        );
        assertEquals(ActivityDomainErrorCode.DOMAIN_NAME_EXISTED.getCode(), ex.getCode());
    }

    // ===== TC7 =====
    @Test
    void updateActivityDomain_fail_subdomain_not_existed() {
        UpdateActivityDomainRequest request = validUpdateRequest();
        activityDomain = validActivityDomain();

        UpdateActivitySubDomainRequest editRequest = new UpdateActivitySubDomainRequest();
        editRequest.setId((short) 999);
        editRequest.setAction("EDIT");
        editRequest.setName("Updated Subdomain");

        request.setActivitySubDomainUpdateRequests(List.of(editRequest));

        when(activityDomainRepository.findById(domainId)).thenReturn(Optional.of(activityDomain));
        when(activityDomainRepository.existsByNameIgnoreCase(request.getName())).thenReturn(false);
        when(activitySubDomainRepository.findById((short) 999)).thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> activityDomainService.updateActivityDomain(domainId, request)
        );
        assertEquals(ActivityDomainErrorCode.SUBDOMAIN_NOT_EXISTED.getCode(), ex.getCode());
    }

    // ==== getActivityDomains ===================================
    // ===== TC1 =====
    @Test
    void getActivityDomains_success_with_active_filter() {
        int pageNumber = 0;
        int pageSize = 10;
        String inputActive = "true";
        String name = "Domain";

        ActivityDomain domain = new ActivityDomain();
        Page<ActivityDomain> mockPage = new PageImpl<>(List.of(domain));

        when(activityDomainRepository.search(eq(true), eq("Domain"), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<ActivityDomainDetailsResponse> result =
                activityDomainService.getActivityDomains(pageNumber, pageSize, inputActive, name);

        assertEquals(1, result.getTotalElements());
        verify(activityDomainRepository).search(eq(true), eq("Domain"), any(Pageable.class));
    }

    // ===== TC2 =====
    @Test
    void getActivityDomains_success_no_filters() {
        int pageNumber = 0;
        int pageSize = 10;

        Page<ActivityDomain> mockPage = new PageImpl<>(List.of(new ActivityDomain()));

        when(activityDomainRepository.search(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<ActivityDomainDetailsResponse> result =
                activityDomainService.getActivityDomains(pageNumber, pageSize, null, null);

        assertEquals(1, result.getTotalElements());
        verify(activityDomainRepository).search(isNull(), isNull(), any(Pageable.class));
    }

    // ==== changeActivityDomainVisibility ===================================
    // ===== TC1 =====
    @Test
    void changeActivityDomainVisibility_success() {
        ChangeActivityDomainVisibilityRequest request = validChangeVisibilityRequest();
        activityDomain = validActivityDomain();

        when(activityDomainRepository.findById(domainId)).thenReturn(Optional.of(activityDomain));

        activityDomainService.changeActivityDomainVisibility(domainId, request);

        verify(activityDomainRepository).save(activityDomain);
    }

    // ===== TC2 =====
    @Test
    void changeActivityDomainVisibility_fail_domain_not_existed() {
        ChangeActivityDomainVisibilityRequest request = validChangeVisibilityRequest();
        when(activityDomainRepository.findById(domainId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> activityDomainService.changeActivityDomainVisibility(domainId, request)
        );
        assertEquals(ActivityDomainErrorCode.DOMAIN_NOT_EXISTED.getCode(), ex.getCode());
    }

    // ==== changeActivitySubDomainVisibility ===================================
    // ===== TC1 =====
    @Test
    void changeActivitySubDomainVisibility_success() {
        ChangeActivitySubDomainVisibilityRequest request = validChangeSubDomainVisibilityRequest();
        activitySubDomain = validActivitySubDomain();

        when(activitySubDomainRepository.findById((short) 1)).thenReturn(Optional.of(activitySubDomain));

        activityDomainService.changeActivitySubDomainVisibility((short) 1, request);

        verify(activitySubDomainRepository).save(activitySubDomain);
    }

    // ===== TC2 =====
    @Test
    void changeActivitySubDomainVisibility_fail_subdomain_not_existed() {
        ChangeActivitySubDomainVisibilityRequest request = validChangeSubDomainVisibilityRequest();
        when(activitySubDomainRepository.findById((short) 1)).thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> activityDomainService.changeActivitySubDomainVisibility(domainId, request)
        );
        assertEquals(ActivityDomainErrorCode.SUBDOMAIN_NOT_EXISTED.getCode(), ex.getCode());
    }
}

