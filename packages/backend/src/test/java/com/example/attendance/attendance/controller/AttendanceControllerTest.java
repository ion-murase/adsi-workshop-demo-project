package com.example.attendance.attendance.controller;

import com.example.attendance.attendance.domain.AttendanceStatus;
import com.example.attendance.attendance.dto.AttendanceHistoryResponse;
import com.example.attendance.attendance.dto.AttendanceRecordResponse;
import com.example.attendance.attendance.dto.DailyAttendanceResponse;
import com.example.attendance.attendance.dto.MonthlySummaryResponse;
import com.example.attendance.attendance.dto.TodayStatusResponse;
import com.example.attendance.attendance.service.AttendanceService;
import com.example.attendance.common.config.CorsConfig;
import com.example.attendance.common.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.MediaType;

@WebMvcTest(
    controllers = AttendanceController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {SecurityConfig.class, CorsConfig.class}
    )
)
@Import(AttendanceControllerTest.TestSecurityConfig.class)
@ActiveProfiles("test")
class AttendanceControllerTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    private static final UUID EMPLOYEE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("POST /api/attendance/clock-in は201を返す")
    void clockIn_validRequest_returns201() throws Exception {
        // Arrange
        var response = new AttendanceRecordResponse(
                UUID.randomUUID(),
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-15T00:00:00Z"),
                null,
                false,
                null
        );
        when(attendanceService.clockIn(EMPLOYEE_ID, null)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/attendance/clock-in")
                        .param("employeeId", EMPLOYEE_ID.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workDate").value("2025-01-15"))
                .andExpect(jsonPath("$.clockOut").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/attendance/clock-out は200を返す")
    void clockOut_validRequest_returns200() throws Exception {
        // Arrange
        var response = new AttendanceRecordResponse(
                UUID.randomUUID(),
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-14T23:00:00Z"),
                Instant.parse("2025-01-15T08:00:00Z"),
                false,
                null
        );
        when(attendanceService.clockOut(EMPLOYEE_ID)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/attendance/clock-out")
                        .param("employeeId", EMPLOYEE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clockOut").exists());
    }

    @Test
    @DisplayName("GET /api/attendance/today は200を返す")
    void getTodayStatus_validRequest_returns200() throws Exception {
        // Arrange
        var response = new TodayStatusResponse(AttendanceStatus.NOT_CLOCKED_IN, List.of());
        when(attendanceService.getTodayStatus(EMPLOYEE_ID)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/attendance/today")
                        .param("employeeId", EMPLOYEE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_CLOCKED_IN"))
                .andExpect(jsonPath("$.records").isArray());
    }

    @Test
    @DisplayName("POST /api/attendance/clock-in メモ付きは201を返しmemoが含まれる")
    void clockIn_withMemo_returns201WithMemo() throws Exception {
        // Arrange
        var response = new AttendanceRecordResponse(
                UUID.randomUUID(),
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-15T00:00:00Z"),
                null,
                false,
                "在宅勤務"
        );
        when(attendanceService.clockIn(EMPLOYEE_ID, "在宅勤務")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/attendance/clock-in")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"在宅勤務\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memo").value("在宅勤務"));
    }

    @Test
    @DisplayName("PUT /api/attendance/memo は200を返す")
    void updateMemo_validRequest_returns200() throws Exception {
        // Arrange
        var recordId = UUID.randomUUID();
        var response = new AttendanceRecordResponse(
                recordId,
                LocalDate.of(2025, 1, 15),
                Instant.parse("2025-01-15T00:00:00Z"),
                null,
                false,
                "遅刻：電車遅延"
        );
        when(attendanceService.updateMemo(recordId, EMPLOYEE_ID, "遅刻：電車遅延")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/attendance/memo")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attendanceRecordId\":\"" + recordId + "\",\"memo\":\"遅刻：電車遅延\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memo").value("遅刻：電車遅延"));
    }

    @Test
    @DisplayName("GET /api/attendance/history は200を返す")
    void getHistory_validRequest_returns200() throws Exception {
        // Arrange
        var dailyResponse = new DailyAttendanceResponse(
                LocalDate.of(2025, 1, 15),
                List.of(),
                540,
                60,
                480,
                0
        );
        var summary = new MonthlySummaryResponse(1, 480, 0, 22);
        var response = new AttendanceHistoryResponse("2025-01", List.of(dailyResponse), summary);
        when(attendanceService.getHistory(EMPLOYEE_ID, "2025-01")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/attendance/history")
                        .param("employeeId", EMPLOYEE_ID.toString())
                        .param("month", "2025-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2025-01"))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.summary.workDays").value(1));
    }
}
