package com.xp.starsite.health.controller;

import com.xp.starsite.common.result.R;
import com.xp.starsite.health.dto.IndicatorBatchDTO;
import com.xp.starsite.health.dto.ReportCreateDTO;
import com.xp.starsite.health.service.HealthService;
import com.xp.starsite.health.vo.HealthReportVO;
import com.xp.starsite.health.vo.PageVO;
import com.xp.starsite.health.vo.TrendPointVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 体检报告接口（全部需 JWT）
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @PostMapping("/reports")
    public R<HealthReportVO> create(@Valid @RequestBody ReportCreateDTO dto) {
        return R.ok(healthService.createReport(dto));
    }

    @GetMapping("/reports")
    public R<PageVO<HealthReportVO>> list(@RequestParam(required = false) Long familyMemberId,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return R.ok(healthService.listReports(familyMemberId, year, page, size));
    }

    @GetMapping("/reports/{id}")
    public R<HealthReportVO> detail(@PathVariable Long id) {
        return R.ok(healthService.getDetail(id));
    }

    @PostMapping("/reports/{id}/indicators")
    public R<Void> addIndicators(@PathVariable Long id, @Valid @RequestBody IndicatorBatchDTO dto) {
        healthService.addIndicators(id, dto);
        return R.ok();
    }

    @DeleteMapping("/reports/{id}")
    public R<Void> delete(@PathVariable Long id) {
        healthService.deleteReport(id);
        return R.ok();
    }

    @GetMapping("/trends")
    public R<List<TrendPointVO>> trends(@RequestParam Long familyMemberId,
                                        @RequestParam String itemName) {
        return R.ok(healthService.trends(familyMemberId, itemName));
    }

    @GetMapping("/search")
    public R<List<HealthReportVO>> search(@RequestParam String keyword,
                                          @RequestParam(required = false) Long familyMemberId) {
        return R.ok(healthService.search(keyword, familyMemberId));
    }
}
