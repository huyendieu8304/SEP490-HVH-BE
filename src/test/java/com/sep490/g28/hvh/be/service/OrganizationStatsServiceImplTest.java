package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.host.payload.TopHostPayload;
import com.sep490.g28.hvh.be.dto.organizationstats.response.OrganizationCountHostsAndEventsResponse;
import com.sep490.g28.hvh.be.dto.organizationstats.response.OrganizationStatsResponseForManager;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.Host;
import com.sep490.g28.hvh.be.entity.Organization;
import com.sep490.g28.hvh.be.entity.OrganizationStats;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.repository.HostRepository;
import com.sep490.g28.hvh.be.repository.OrganizationRepository;
import com.sep490.g28.hvh.be.repository.OrganizationStatsRepository;
import com.sep490.g28.hvh.be.service.impl.OrganizationStatsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrganizationStatsServiceImplTest {
    @InjectMocks
    OrganizationStatsServiceImpl service;

    @Mock
    OrganizationStatsRepository organizationStatsRepository;

    @Mock
    OrganizationRepository organizationRepository;

    @Mock
    EventRepository eventRepository;

    @Mock
    HostRepository hostRepository;

    @Mock
    CurrentUserProvider currentUserProvider;

    private Organization mockOrg() {
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(UUID.randomUUID());
        return org;
    }

    //===== updateOrganizationStats ===============

    @Test
    void updateOrganizationStats_whenUpdated_shouldNotInsert() {
        when(organizationStatsRepository.updateOrganizationStatsBy(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()
        )).thenReturn(1);

        service.updateOrganizationCreditHoursAndCountApplicationsAndCountCompletedEventStats(
                UUID.randomUUID(), 1, 2025, 1, 1, 1
        );

        verify(organizationStatsRepository, never()).save(any());
    }

    @Test
    void updateOrganizationStats_whenNotUpdated_shouldInsert() {
        UUID orgId = UUID.randomUUID();

        when(organizationStatsRepository.updateOrganizationStatsBy(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()
        )).thenReturn(0);

        service.updateOrganizationCreditHoursAndCountApplicationsAndCountCompletedEventStats(
                orgId, 1, 2025, 2, 3, 4
        );

        verify(organizationStatsRepository).save(argThat(stats ->
                stats.getOrgId().equals(orgId) &&
                        stats.getCompletedEvents() == 1 &&
                        stats.getApprovedApplications() == 2 &&
                        stats.getAttendedApplications() == 3 &&
                        stats.getCreditHours() == 4
        ));
    }


    // ========= compileOrganizationsMonthlyStatistics ==============================
    private Event mockEvent(UUID orgId, UUID hostId, int credit, int approved, int attended) {
        Event e = mock(Event.class);
        Organization org = mock(Organization.class);
        Host host = mock(Host.class);

        when(org.getId()).thenReturn(orgId);
        when(host.getId()).thenReturn(hostId);
        lenient().when(host.getFullName()).thenReturn("host");
        lenient().when(host.getEmail()).thenReturn("mail");

        when(e.getOrganization()).thenReturn(org);
        when(e.getHost()).thenReturn(host);

        when(e.getTotalCreditHours()).thenReturn(credit);
        when(e.getTotalApprovedApplications()).thenReturn(approved);
        when(e.getTotalAttendedApplications()).thenReturn(attended);

        return e;
    }

    @Test
    void compileStats_emptyEventList_shouldDoNothing() {
        when(eventRepository.getCompletedEventBetween(any(), any()))
                .thenReturn(List.of());

        service.compileOrganizationsMonthlyStatistics();

        verify(organizationStatsRepository, never()).save(any());
        verify(organizationStatsRepository, never()).updateOrganizationStatsBy(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any()
        );
    }

    @Test
    void compileStats_newOrg_shouldInsert() {
        UUID orgId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();

        Event event = mockEvent(orgId, hostId, 1, 1, 1);

        when(eventRepository.getCompletedEventBetween(any(), any()))
                .thenReturn(List.of(event));

        when(organizationStatsRepository.updateOrganizationStatsBy(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any()
        )).thenReturn(0);

        service.compileOrganizationsMonthlyStatistics();

        verify(organizationStatsRepository).save(any(OrganizationStats.class));
    }

    @Test
    void compileStats_existingOrg_shouldOnlyUpdate() {
        UUID orgId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();

        Event event = mockEvent(orgId, hostId, 2, 2, 2);

        when(eventRepository.getCompletedEventBetween(any(), any()))
                .thenReturn(List.of(event));

        when(organizationStatsRepository.updateOrganizationStatsBy(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any()
        )).thenReturn(1);

        service.compileOrganizationsMonthlyStatistics();

        verify(organizationStatsRepository, never()).save(any());
    }

    @Test
    void compileStats_sameOrg_shouldAggregate() {
        UUID orgId = UUID.randomUUID();

        Event e1 = mockEvent(orgId, UUID.randomUUID(), 1, 1, 1);
        Event e2 = mockEvent(orgId, UUID.randomUUID(), 2, 2, 2);

        when(eventRepository.getCompletedEventBetween(any(), any()))
                .thenReturn(List.of(e1, e2));

        when(organizationStatsRepository.updateOrganizationStatsBy(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any()
        )).thenReturn(1);

        service.compileOrganizationsMonthlyStatistics();

        verify(organizationStatsRepository).updateOrganizationStatsBy(
                eq(orgId),
                anyInt(),
                anyInt(),
                eq(3), eq(3), eq(3), eq(2), any()
        );
    }

    @Test
    void compileStats_multipleOrgs_shouldCreateMultipleStats() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();

        Event e1 = mockEvent(orgA, UUID.randomUUID(), 1, 1, 1);
        Event e2 = mockEvent(orgB, UUID.randomUUID(), 2, 2, 2);

        when(eventRepository.getCompletedEventBetween(any(), any()))
                .thenReturn(List.of(e1, e2));

        when(organizationStatsRepository.updateOrganizationStatsBy(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any()
        )).thenReturn(0);

        service.compileOrganizationsMonthlyStatistics();

        verify(organizationStatsRepository, times(2))
                .save(any(OrganizationStats.class));
    }

    @Test
    void compileStats_newHost_shouldAddPayload() {
        UUID orgId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();

        Event event = mockEvent(orgId, hostId, 3, 3, 3);

        when(eventRepository.getCompletedEventBetween(any(), any()))
                .thenReturn(List.of(event));

        when(organizationStatsRepository.updateOrganizationStatsBy(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any()
        )).thenReturn(0);

        service.compileOrganizationsMonthlyStatistics();

        verify(organizationStatsRepository).save(argThat(stats ->
                stats.getTopHostPayloads() != null &&
                        stats.getTopHostPayloads().size() == 1
        ));
    }

    @Test
    void compileStats_existingHost_shouldIncreaseValues() {
        UUID orgId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();

        Event e1 = mockEvent(orgId, hostId, 1, 0, 0);
        Event e2 = mockEvent(orgId, hostId, 2, 0, 0);

        when(eventRepository.getCompletedEventBetween(any(), any()))
                .thenReturn(List.of(e1, e2));

        when(organizationStatsRepository.updateOrganizationStatsBy(
                any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any()
        )).thenReturn(0);

        service.compileOrganizationsMonthlyStatistics();

        verify(organizationStatsRepository).save(argThat(stats ->
                stats.getTopHostPayloads().get(0).getTotalEvent() == 2
        ));
    }

    @Test
    void compileStats_shouldSortTopHosts() {
        UUID orgId = UUID.randomUUID();

        Event e1 = mockEvent(orgId, UUID.randomUUID(), 5, 0, 0);
        Event e2 = mockEvent(orgId, UUID.randomUUID(), 10, 0, 0);

        when(eventRepository.getCompletedEventBetween(any(), any()))
                .thenReturn(List.of(e1, e2));

        when(organizationStatsRepository.updateOrganizationStatsBy(any(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(0);

        service.compileOrganizationsMonthlyStatistics();

        ArgumentCaptor<OrganizationStats> captor =
                ArgumentCaptor.forClass(OrganizationStats.class);

        verify(organizationStatsRepository).save(captor.capture());

        List<TopHostPayload> list = captor.getValue().getTopHostPayloads();

        assertThat(list)
                .extracting(TopHostPayload::getTotalCreditHour)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    // ================= getOrganizations6MonthsStatistics =================

    @Test
    void get6MonthsStats_shouldMapCorrectly() {
        UUID orgId = UUID.randomUUID();

        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(orgId);

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(organizationRepository.findByOrganizationManager_Id(any()))
                .thenReturn(org);

        OrganizationStats stats = new OrganizationStats();
        stats.setYear(2026);
        stats.setMonth(4);
        stats.setCompletedEvents(2);
        stats.setCreditHours(10);
        stats.setApprovedApplications(5);
        stats.setAttendedApplications(3);
        stats.setTopHostPayloads(List.of());

        when(organizationStatsRepository.findLast6MonthsStats(eq(orgId), anyInt()))
                .thenReturn(List.of(stats));

        List<OrganizationStatsResponseForManager> result =
                service.getOrganizations6MonthsStatistics();

        assertThat(result).hasSize(1);

        OrganizationStatsResponseForManager r = result.get(0);

        assertThat(r.getYear()).isEqualTo(2026);
        assertThat(r.getMonth()).isEqualTo(4);
        assertThat(r.getCompletedEvents()).isEqualTo(2);
        assertThat(r.getCreditHours()).isEqualTo(10);
        assertThat(r.getApprovedApplications()).isEqualTo(5);
        assertThat(r.getAttendedApplications()).isEqualTo(3);
    }

    @Test
    void get6MonthsStats_emptyList_shouldReturnEmpty() {
        UUID orgId = UUID.randomUUID();

        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(orgId);

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(organizationRepository.findByOrganizationManager_Id(any()))
                .thenReturn(org);

        when(organizationStatsRepository.findLast6MonthsStats(eq(orgId), anyInt()))
                .thenReturn(List.of());

        List<OrganizationStatsResponseForManager> result =
                service.getOrganizations6MonthsStatistics();

        assertThat(result).isEmpty();
    }

    // ================= countHostsAndEvents =================

    @Test
    void countHostsAndEvents_shouldReturnCombinedResult() {
        UUID orgId = UUID.randomUUID();

        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(orgId);

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(organizationRepository.findByOrganizationManager_Id(any()))
                .thenReturn(org);

        OrganizationCountHostsAndEventsResponse response =
                new OrganizationCountHostsAndEventsResponse();
        response.setRecruitingEventsCount(5);

        when(eventRepository.countEventsByOrg(orgId))
                .thenReturn(response);

        when(hostRepository.countByOrganizationId(orgId))
                .thenReturn(10);

        OrganizationCountHostsAndEventsResponse result =
                service.countHostsAndEvents();

        assertThat(result.getRecruitingEventsCount()).isEqualTo(5);
        assertThat(result.getHostsCount()).isEqualTo(10);
    }

    @Test
    void countHostsAndEvents_shouldHandleZeroData() {
        UUID orgId = UUID.randomUUID();

        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(orgId);

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(organizationRepository.findByOrganizationManager_Id(any()))
                .thenReturn(org);

        OrganizationCountHostsAndEventsResponse response =
                new OrganizationCountHostsAndEventsResponse();

        when(eventRepository.countEventsByOrg(orgId))
                .thenReturn(response);

        when(hostRepository.countByOrganizationId(orgId))
                .thenReturn(0);

        OrganizationCountHostsAndEventsResponse result =
                service.countHostsAndEvents();

        assertThat(result.getHostsCount()).isEqualTo(0);
    }
}
