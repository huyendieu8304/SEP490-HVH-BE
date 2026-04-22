package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.systemstats.response.SystemStatsResponse;

import java.util.List;

public interface SystemStatsService {

    void compileSystemStatsDaily();

    //todo remove this after mock data
    void compileSystemStatsMonthly(int year, int month);

    List<SystemStatsResponse> getSystem6MonthsStatistics();
}
