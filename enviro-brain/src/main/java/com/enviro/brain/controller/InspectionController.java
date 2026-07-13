package com.enviro.brain.controller;

import com.enviro.brain.dto.ApiResponse;
import com.enviro.brain.dto.InspectionContext;
import com.enviro.brain.service.InspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inspections")
@RequiredArgsConstructor
@Slf4j
public class InspectionController {

    private final InspectionService inspectionService;

    @PostMapping("/trigger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trigger(
            @RequestParam(required = false) String scenario) {
        String sc = (scenario == null || scenario.isBlank()) ? "enviro" : scenario;
        log.info("[Controller] 手动触发巡检，场景={}", sc);
        InspectionContext ctx = inspectionService.prepareInspection("manual", sc);
        inspectionService.runInspectionAsync(ctx);
        return ResponseEntity.accepted().body(
            ApiResponse.success(Map.of("taskId", ctx.getInspectId(), "scenario", sc, "status", "running")));
    }
}
