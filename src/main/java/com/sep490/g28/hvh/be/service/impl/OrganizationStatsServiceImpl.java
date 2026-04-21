package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.dto.host.payload.TopHostPayload;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.OrganizationStats;
import com.sep490.g28.hvh.be.repository.EventRepository;
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
    EventRepository eventRepository;

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
        }
    }

}
