package com.enviro.brain.controller;

import com.enviro.brain.config.WebMvcConfig;
import com.enviro.brain.service.InspectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InspectionController.class)
@Import(WebMvcConfig.class)
@TestPropertySource(properties = "enviro.api.key=integration-test-key")
@DisplayName("InspectionController")
class InspectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InspectionService inspectionService;

    @Test
    @DisplayName("POST /trigger → 202 with taskId")
    void trigger_shouldReturn202() throws Exception {
        when(inspectionService.executeInspection(anyString())).thenReturn(42L);

        mockMvc.perform(post("/api/v1/inspections/trigger")
                        .header("X-API-Key", "integration-test-key")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(42));
    }

    @Test
    @DisplayName("POST /trigger without API Key → 401")
    void triggerWithoutApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/inspections/trigger")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
