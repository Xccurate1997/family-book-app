package com.bookapp.controller;

import com.bookapp.service.StatsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/monthly-trend")
    public List<StatsService.MonthlyTrendItem> monthlyTrend(
            @RequestParam Long ledgerId,
            @RequestParam(defaultValue = "6") int months) {
        return statsService.monthlyTrend(ledgerId, months);
    }

    @GetMapping("/category-breakdown")
    public List<StatsService.CategoryBreakdownItem> categoryBreakdown(
            @RequestParam Long ledgerId,
            @RequestParam int year,
            @RequestParam int month) {
        return statsService.categoryBreakdown(ledgerId, year, month);
    }

    @GetMapping("/daily")
    public List<StatsService.DailyStatItem> daily(
            @RequestParam Long ledgerId,
            @RequestParam int year,
            @RequestParam int month) {
        return statsService.dailyStats(ledgerId, year, month);
    }

    @GetMapping("/yearly-summary")
    public StatsService.YearlySummaryItem yearlySummary(
            @RequestParam Long ledgerId,
            @RequestParam int year) {
        return statsService.yearlySummary(ledgerId, year);
    }

    @GetMapping("/yearly-monthly-trend")
    public List<StatsService.MonthlyTrendItem> yearlyMonthlyTrend(
            @RequestParam Long ledgerId,
            @RequestParam int year) {
        return statsService.yearlyMonthlyTrend(ledgerId, year);
    }

    @GetMapping("/yearly-category-ranking")
    public List<StatsService.CategoryBreakdownItem> yearlyCategoryRanking(
            @RequestParam Long ledgerId,
            @RequestParam int year,
            @RequestParam(defaultValue = "10") int limit) {
        return statsService.yearlyCategoryRanking(ledgerId, year, limit);
    }
}
