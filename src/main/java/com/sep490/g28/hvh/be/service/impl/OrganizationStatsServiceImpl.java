package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.host.payload.TopHostPayload;
import com.sep490.g28.hvh.be.dto.organizationstats.response.OrganizationCountHostsAndEventsResponse;
import com.sep490.g28.hvh.be.dto.organizationstats.response.OrganizationStatsResponseForManager;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.Organization;
import com.sep490.g28.hvh.be.entity.OrganizationStats;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.repository.HostRepository;
import com.sep490.g28.hvh.be.repository.OrganizationRepository;
import com.sep490.g28.hvh.be.repository.OrganizationStatsRepository;
import com.sep490.g28.hvh.be.service.OrganizationStatsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrganizationStatsServiceImpl implements OrganizationStatsService {
    OrganizationStatsRepository organizationStatsRepository;
    OrganizationRepository organizationRepository;
    EventRepository eventRepository;
    HostRepository hostRepository;

    CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public void updateOrganizationCreditHoursAndCountApplicationsAndCountCompletedEventStats(
            UUID orgId,
            int month,
            int year,
            int addApprovedApplications,
            int addAttendedApplications,
            int addCreditHours
    ){
        int updated = organizationStatsRepository.updateOrganizationStatsBy(
                orgId,
                month,
                year,
                addApprovedApplications,
                addAttendedApplications,
                addCreditHours,
                1
        );

        // nếu chưa có record thì insert
        if (updated == 0) {
            OrganizationStats stats = new OrganizationStats();
            stats.setOrgId(orgId);
            stats.setMonth(month);
            stats.setYear(year);
            stats.setApprovedApplications(addApprovedApplications);
            stats.setAttendedApplications(addAttendedApplications);
            stats.setCreditHours(addCreditHours);
            stats.setCompletedEvents(1);

            organizationStatsRepository.save(stats);
        }
    }

    @Override
    @Transactional
    public void compileOrganizationsMonthlyStatistics() {
        //recalculate previous month's stats
        YearMonth prev = YearMonth.now().minusMonths(1);
        LocalDate startDate = prev.atDay(1).minusDays(2);
        LocalDate endDate = prev.atEndOfMonth().minusDays(2);

        //search for events that has end date in the previous months
        List<Event> eventsEndedInLastMonth = eventRepository.getCompletedEventBetween(startDate, endDate);

        Map<UUID, OrganizationStats> organizationStatsMap = new HashMap<>();

        //categorize by organization, sum up the data, insert into statistic
        for (Event event : eventsEndedInLastMonth) {
            UUID orgId = event.getOrganization().getId();

            OrganizationStats stats = organizationStatsMap.get(orgId);

            if (stats == null) {
                stats = new OrganizationStats();
                stats.setOrgId(orgId);
                stats.setMonth(prev.getMonthValue());
                stats.setYear(prev.getYear());
                stats.setCompletedEvents(1);
                stats.setCreditHours(event.getTotalCreditHours());
                stats.setApprovedApplications(event.getTotalApprovedApplications());
                stats.setAttendedApplications(event.getTotalAttendedApplications());

                TopHostPayload payload = new TopHostPayload();
                payload.setHostId(event.getHost().getId());
                payload.setFullName(event.getHost().getFullName());
                payload.setEmail(event.getHost().getEmail());
                payload.setTotalEvent(1);
                payload.setTotalCreditHour(event.getTotalCreditHours());
                stats.setTopHostPayloads(new ArrayList<>(List.of(payload)));

                organizationStatsMap.put(orgId, stats);
            } else {
                // add up stats
                stats.setCompletedEvents(stats.getCompletedEvents() + 1);
                stats.setCreditHours(stats.getCreditHours() + event.getTotalCreditHours());
                stats.setApprovedApplications(stats.getApprovedApplications() + event.getTotalApprovedApplications());
                stats.setAttendedApplications(stats.getAttendedApplications() + event.getTotalAttendedApplications());

                // update top hosts
                List<TopHostPayload> hosts = stats.getTopHostPayloads();
//                if (hosts == null) {
//                    hosts = new ArrayList<>();
//                    stats.setTopHostPayloads(hosts);
//                }

                UUID hostId = event.getHost().getId();

                TopHostPayload existing = hosts.stream()
                        .filter(h -> h.getHostId().equals(hostId))
                        .findFirst()
                        .orElse(null);

                if (existing == null) {
                    TopHostPayload payload = new TopHostPayload();
                    payload.setHostId(hostId);
                    payload.setFullName(event.getHost().getFullName());
                    payload.setEmail(event.getHost().getEmail());
                    payload.setTotalEvent(1);
                    payload.setTotalCreditHour(event.getTotalCreditHours());

                    hosts.add(payload);
                } else {
                    existing.setTotalEvent(existing.getTotalEvent() + 1);
                    existing.setTotalCreditHour(existing.getTotalCreditHour() + event.getTotalCreditHours());
                }
            }
        }

        //sort and get top 5 host
        for (OrganizationStats stats : organizationStatsMap.values()) {
            List<TopHostPayload> hosts = stats.getTopHostPayloads();

            if (hosts == null || hosts.isEmpty()) continue;

            List<TopHostPayload> top5 = hosts.stream()
                    .sorted(Comparator
                            .comparing(TopHostPayload::getTotalCreditHour).reversed()
                            .thenComparing(TopHostPayload::getTotalEvent).reversed()
                    )
                    .limit(5)
                    .toList();

            stats.setTopHostPayloads(top5);
        }

        for (OrganizationStats stats : organizationStatsMap.values()) {
            int updated = organizationStatsRepository.updateOrganizationStatsBy(
                    stats.getOrgId(),
                    stats.getMonth(),
                    stats.getYear(),
                    stats.getApprovedApplications(),
                    stats.getAttendedApplications(),
                    stats.getCreditHours(),
                    stats.getCompletedEvents(),
                    stats.getTopHostPayloads()
            );

            // nếu chưa có record thì insert
            if (updated == 0) {
                organizationStatsRepository.save(stats);
            }
            log.info("Recalculate monthly stats of organization, orgId={}", stats.getOrgId());
        }
    }

    //todo, remove this method after finish mock data
    @Override
    @Transactional
    public void compileOrganizationsMonthlyStatistics(int year, int month) {
        //recalculate previous month's stats
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1).minusDays(2);
        LocalDate endDate = yearMonth.atEndOfMonth().minusDays(2);

        //search for events that has end date in the previous months
        List<Event> eventsEndedInLastMonth = eventRepository.getCompletedEventBetween(startDate, endDate);

        Map<UUID, OrganizationStats> organizationStatsMap = new HashMap<>();

        //categorize by organization, sum up the data, insert into statistic
        for (Event event : eventsEndedInLastMonth) {
            UUID orgId = event.getOrganization().getId();

            OrganizationStats stats = organizationStatsMap.get(orgId);

            if (stats == null) {
                stats = new OrganizationStats();
                stats.setOrgId(orgId);
                stats.setMonth(month);
                stats.setYear(year);
                stats.setCompletedEvents(1);
                stats.setCreditHours(event.getTotalCreditHours());
                stats.setApprovedApplications(event.getTotalApprovedApplications());
                stats.setAttendedApplications(event.getTotalAttendedApplications());

                TopHostPayload payload = new TopHostPayload();
                payload.setHostId(event.getHost().getId());
                payload.setFullName(event.getHost().getFullName());
                payload.setEmail(event.getHost().getEmail());
                payload.setTotalEvent(1);
                payload.setTotalCreditHour(event.getTotalCreditHours());
                stats.setTopHostPayloads(new ArrayList<>(List.of(payload)));

                organizationStatsMap.put(orgId, stats);
            } else {
                // add up stats
                stats.setCompletedEvents(stats.getCompletedEvents() + 1);
                stats.setCreditHours(stats.getCreditHours() + event.getTotalCreditHours());
                stats.setApprovedApplications(stats.getApprovedApplications() + event.getTotalApprovedApplications());
                stats.setAttendedApplications(stats.getAttendedApplications() + event.getTotalAttendedApplications());

                // update top hosts
                List<TopHostPayload> hosts = stats.getTopHostPayloads();
//                if (hosts == null) {
//                    hosts = new ArrayList<>();
//                    stats.setTopHostPayloads(hosts);
//                }

                UUID hostId = event.getHost().getId();

                TopHostPayload existing = hosts.stream()
                        .filter(h -> h.getHostId().equals(hostId))
                        .findFirst()
                        .orElse(null);

                if (existing == null) {
                    TopHostPayload payload = new TopHostPayload();
                    payload.setHostId(hostId);
                    payload.setFullName(event.getHost().getFullName());
                    payload.setEmail(event.getHost().getEmail());
                    payload.setTotalEvent(1);
                    payload.setTotalCreditHour(event.getTotalCreditHours());

                    hosts.add(payload);
                } else {
                    existing.setTotalEvent(existing.getTotalEvent() + 1);
                    existing.setTotalCreditHour(existing.getTotalCreditHour() + event.getTotalCreditHours());
                }
            }
        }

        //sort and get top 5 host
        for (OrganizationStats stats : organizationStatsMap.values()) {
            List<TopHostPayload> hosts = stats.getTopHostPayloads();

            if (hosts == null || hosts.isEmpty()) continue;

            List<TopHostPayload> top5 = hosts.stream()
                    .sorted(Comparator
                            .comparing(TopHostPayload::getTotalCreditHour).reversed()
                            .thenComparing(TopHostPayload::getTotalEvent).reversed()
                    )
                    .limit(5)
                    .toList();

            stats.setTopHostPayloads(top5);
        }

        for (OrganizationStats stats : organizationStatsMap.values()) {
            int updated = organizationStatsRepository.updateOrganizationStatsBy(
                    stats.getOrgId(),
                    stats.getMonth(),
                    stats.getYear(),
                    stats.getApprovedApplications(),
                    stats.getAttendedApplications(),
                    stats.getCreditHours(),
                    stats.getCompletedEvents(),
                    stats.getTopHostPayloads()
            );

            // nếu chưa có record thì insert
            if (updated == 0) {
                organizationStatsRepository.save(stats);
            }
        }
    }

    @Override
    public List<OrganizationStatsResponseForManager> getOrganizations6MonthsStatistics() {

        Organization organization = organizationRepository.findByOrganizationManager_Id(currentUserProvider.getId());

        YearMonth now = YearMonth.now();
        YearMonth from = now.minusMonths(5); // tổng 6 tháng

        int fromYm = from.getYear() * 100 + from.getMonthValue();

        List<OrganizationStats> statsList =  organizationStatsRepository.findLast6MonthsStats(organization.getId(), fromYm);

        return statsList.stream().map((stats) -> {
            OrganizationStatsResponseForManager response = new OrganizationStatsResponseForManager();
            response.setYear(stats.getYear());
            response.setMonth(stats.getMonth());
            response.setCompletedEvents(stats.getCompletedEvents());
            response.setCreditHours(stats.getCreditHours());
            response.setApprovedApplications(stats.getApprovedApplications());
            response.setAttendedApplications(stats.getAttendedApplications());
            response.setTopHostPayloads(stats.getTopHostPayloads());
            return response;
        }).toList();
    }

    @Override
    public OrganizationCountHostsAndEventsResponse countHostsAndEvents() {
        Organization organization = organizationRepository.findByOrganizationManager_Id(currentUserProvider.getId());

        OrganizationCountHostsAndEventsResponse response = eventRepository.countEventsByOrg(organization.getId());
        response.setHostsCount(hostRepository.countByOrganizationId(organization.getId()));

        return response;
    }

}
