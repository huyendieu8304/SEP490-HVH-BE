package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.systemstats.response.SystemStatsResponse;
import com.sep490.g28.hvh.be.entity.OrganizationStats;
import com.sep490.g28.hvh.be.entity.SystemStats;
import com.sep490.g28.hvh.be.repository.OrganizationRepository;
import com.sep490.g28.hvh.be.repository.OrganizationStatsRepository;
import com.sep490.g28.hvh.be.repository.SystemStatsRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.service.impl.SystemStatsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SystemStatsServiceImplTest {

    @InjectMocks
    SystemStatsServiceImpl service;


    @Mock
    OrganizationStatsRepository organizationStatsRepository;

    @Mock
    SystemStatsRepository systemStatsRepository;

    @Mock
    VolunteerRepository volunteerRepository;

    @Mock
    OrganizationRepository organizationRepository;


    private OrganizationStats mockOrgStats(int events, int credit, int approved, int attended) {
        OrganizationStats s = new OrganizationStats();
        s.setCompletedEvents(events);
        s.setCreditHours(credit);
        s.setApprovedApplications(approved);
        s.setAttendedApplications(attended);
        return s;
    }

    // ================= compileSystemStatsDaily =================

    @Test
    void compileStats_shouldCreateNewSystemStats_whenNotExist() {

        when(organizationStatsRepository.findStatsBy(anyInt(), anyInt()))
                .thenReturn(List.of(
                        mockOrgStats(1,2,3,4),
                        mockOrgStats(2,3,4,5)
                ));

        when(systemStatsRepository.findByYearAndMonth(anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        when(volunteerRepository.countCreatedBetween(any(), any())).thenReturn(5);
        when(organizationRepository.countCreatedBetween(any(), any())).thenReturn(3);

        service.compileSystemStatsDaily();

        ArgumentCaptor<SystemStats> captor =
                ArgumentCaptor.forClass(SystemStats.class);

        verify(systemStatsRepository).save(captor.capture());

        SystemStats result = captor.getValue();

        assertThat(result.getCompletedEvents()).isEqualTo(3);
        assertThat(result.getCreditHours()).isEqualTo(5);
        assertThat(result.getApprovedApplications()).isEqualTo(7);
        assertThat(result.getAttendedApplications()).isEqualTo(9);
        assertThat(result.getVerifiedVolunteers()).isEqualTo(5);
        assertThat(result.getVerifiedOrganizations()).isEqualTo(3);
    }

    @Test
    void compileStats_shouldUpdateExistingSystemStats() {

        SystemStats existing = new SystemStats();
        existing.setVerifiedVolunteers(10);
        existing.setVerifiedOrganizations(20);

        when(organizationStatsRepository.findStatsBy(anyInt(), anyInt()))
                .thenReturn(List.of(mockOrgStats(1,1,1,1)));

        when(systemStatsRepository.findByYearAndMonth(anyInt(), anyInt()))
                .thenReturn(Optional.of(existing));

        when(volunteerRepository.countCreatedBetween(any(), any())).thenReturn(2);
        when(organizationRepository.countCreatedBetween(any(), any())).thenReturn(3);

        service.compileSystemStatsDaily();

        ArgumentCaptor<SystemStats> captor =
                ArgumentCaptor.forClass(SystemStats.class);

        verify(systemStatsRepository).save(captor.capture());

        SystemStats result = captor.getValue();

        assertThat(result.getVerifiedVolunteers()).isEqualTo(12);
        assertThat(result.getVerifiedOrganizations()).isEqualTo(23);
    }

    @Test
    void compileStats_shouldHandleEmptyOrganizationStats() {

        when(organizationStatsRepository.findStatsBy(anyInt(), anyInt()))
                .thenReturn(List.of());

        when(systemStatsRepository.findByYearAndMonth(anyInt(), anyInt()))
                .thenReturn(Optional.empty());

        when(volunteerRepository.countCreatedBetween(any(), any())).thenReturn(0);
        when(organizationRepository.countCreatedBetween(any(), any())).thenReturn(0);

        service.compileSystemStatsDaily();

        verify(systemStatsRepository).save(any(SystemStats.class));
    }

    // ================= getSystem6MonthsStatistics =================

    @Test
    void get6MonthsStats_shouldMapCorrectly() {

        SystemStats stats = new SystemStats();
        stats.setYear(2026);
        stats.setMonth(4);
        stats.setVerifiedOrganizations(10);
        stats.setVerifiedVolunteers(20);
        stats.setCompletedEvents(3);
        stats.setCreditHours(4);
        stats.setApprovedApplications(5);
        stats.setAttendedApplications(6);

        when(systemStatsRepository.findLast6MonthsStats(anyInt()))
                .thenReturn(List.of(stats));

        List<SystemStatsResponse> result =
                service.getSystem6MonthsStatistics();

        assertThat(result).hasSize(1);

        SystemStatsResponse r = result.get(0);

        assertThat(r.getYear()).isEqualTo(2026);
        assertThat(r.getMonth()).isEqualTo(4);
        assertThat(r.getVerifiedOrganizations()).isEqualTo(10);
        assertThat(r.getVerifiedVolunteers()).isEqualTo(20);
        assertThat(r.getCompletedEvents()).isEqualTo(3);
        assertThat(r.getCreditHours()).isEqualTo(4);
    }

    @Test
    void get6MonthsStats_shouldReturnEmptyList() {

        when(systemStatsRepository.findLast6MonthsStats(anyInt()))
                .thenReturn(List.of());

        List<SystemStatsResponse> result =
                service.getSystem6MonthsStatistics();

        assertThat(result).isEmpty();
    }
}
