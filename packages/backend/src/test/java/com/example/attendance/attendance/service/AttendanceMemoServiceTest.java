package com.example.attendance.attendance.service;

import com.example.attendance.attendance.entity.AttendanceRecord;
import com.example.attendance.attendance.repository.AttendanceRecordRepository;
import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import com.example.attendance.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("打刻メモ機能")
class AttendanceMemoServiceTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2025-01-15T00:00:00Z");
    private static final ZoneId ZONE_TOKYO = ZoneId.of("Asia/Tokyo");
    private static final LocalDate TODAY_TOKYO = LocalDate.of(2025, 1, 15);

    @Mock
    private AttendanceRecordRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private AttendanceServiceImpl service;

    private Employee employee;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(FIXED_INSTANT, ZONE_TOKYO);
        service = new AttendanceServiceImpl(attendanceRepository, employeeRepository, clock);

        var department = Department.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .build();

        employee = Employee.builder()
                .id(UUID.randomUUID())
                .name("田中太郎")
                .email("tanaka@example.com")
                .password("hashed")
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2024, 4, 1))
                .build();
    }

    @Nested
    @DisplayName("出勤打刻時のメモ保存")
    class ClockInWithMemo {

        @Test
        @DisplayName("メモ付きで出勤打刻すると、メモが保存される")
        void clockIn_withMemo_savesMemo() {
            // Arrange
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(attendanceRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            var memo = "在宅勤務";

            // Act
            var result = service.clockIn(employee.getId(), memo);

            // Assert
            assertThat(result.memo()).isEqualTo("在宅勤務");

            var captor = ArgumentCaptor.forClass(AttendanceRecord.class);
            verify(attendanceRepository).save(captor.capture());
            assertThat(captor.getValue().getMemo()).isEqualTo("在宅勤務");
        }

        @Test
        @DisplayName("メモなしで出勤打刻すると、メモはnullで保存される")
        void clockIn_withoutMemo_savesNullMemo() {
            // Arrange
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(attendanceRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.clockIn(employee.getId(), null);

            // Assert
            assertThat(result.memo()).isNull();
        }

        @Test
        @DisplayName("300文字ちょうどのメモは正常に保存される")
        void clockIn_memo300Chars_savesSuccessfully() {
            // Arrange
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            when(attendanceRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            var memo300 = "あ".repeat(300);

            // Act
            var result = service.clockIn(employee.getId(), memo300);

            // Assert
            assertThat(result.memo()).isEqualTo(memo300);
        }

        @Test
        @DisplayName("300文字を超えるメモは拒否される")
        void clockIn_memoExceeds300Chars_throwsBadRequest() {
            // Arrange
            when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
            var longMemo = "あ".repeat(301);

            // Act & Assert
            assertThatThrownBy(() -> service.clockIn(employee.getId(), longMemo))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("300");
        }
    }

    @Nested
    @DisplayName("メモ更新")
    class UpdateMemo {

        @Test
        @DisplayName("当日の打刻レコードのメモを更新できる")
        void updateMemo_today_updatesSuccessfully() {
            // Arrange
            var recordId = UUID.randomUUID();
            var record = AttendanceRecord.builder()
                    .id(recordId)
                    .employee(employee)
                    .workDate(TODAY_TOKYO)
                    .clockIn(FIXED_INSTANT)
                    .build();
            when(attendanceRepository.findById(recordId)).thenReturn(Optional.of(record));
            when(attendanceRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.updateMemo(recordId, employee.getId(), "遅刻：電車遅延");

            // Assert
            assertThat(result.memo()).isEqualTo("遅刻：電車遅延");
        }

        @Test
        @DisplayName("当日以外の打刻レコードのメモ更新は拒否される")
        void updateMemo_pastDate_throwsForbidden() {
            // Arrange
            var recordId = UUID.randomUUID();
            var yesterday = TODAY_TOKYO.minusDays(1);
            var record = AttendanceRecord.builder()
                    .id(recordId)
                    .employee(employee)
                    .workDate(yesterday)
                    .clockIn(FIXED_INSTANT.minusSeconds(86400))
                    .build();
            when(attendanceRepository.findById(recordId)).thenReturn(Optional.of(record));

            // Act & Assert
            assertThatThrownBy(() -> service.updateMemo(recordId, employee.getId(), "更新したい"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("today");
        }

        @Test
        @DisplayName("他人の打刻レコードのメモは更新できない")
        void updateMemo_otherEmployee_throwsForbidden() {
            // Arrange
            var recordId = UUID.randomUUID();
            var otherEmployee = Employee.builder()
                    .id(UUID.randomUUID())
                    .name("山田花子")
                    .email("yamada@example.com")
                    .password("hashed")
                    .role(Role.EMPLOYEE)
                    .build();
            var record = AttendanceRecord.builder()
                    .id(recordId)
                    .employee(otherEmployee)
                    .workDate(TODAY_TOKYO)
                    .clockIn(FIXED_INSTANT)
                    .build();
            when(attendanceRepository.findById(recordId)).thenReturn(Optional.of(record));

            // Act & Assert
            assertThatThrownBy(() -> service.updateMemo(recordId, employee.getId(), "不正アクセス"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("300文字ちょうどのメモで更新が成功する")
        void updateMemo_memo300Chars_updatesSuccessfully() {
            // Arrange
            var recordId = UUID.randomUUID();
            var record = AttendanceRecord.builder()
                    .id(recordId)
                    .employee(employee)
                    .workDate(TODAY_TOKYO)
                    .clockIn(FIXED_INSTANT)
                    .build();
            when(attendanceRepository.findById(recordId)).thenReturn(Optional.of(record));
            when(attendanceRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            var memo300 = "あ".repeat(300);

            // Act
            var result = service.updateMemo(recordId, employee.getId(), memo300);

            // Assert
            assertThat(result.memo()).isEqualTo(memo300);
        }

        @Test
        @DisplayName("300文字を超えるメモでの更新は拒否される")
        void updateMemo_memoExceeds300Chars_throwsBadRequest() {
            // Arrange
            var recordId = UUID.randomUUID();
            var record = AttendanceRecord.builder()
                    .id(recordId)
                    .employee(employee)
                    .workDate(TODAY_TOKYO)
                    .clockIn(FIXED_INSTANT)
                    .build();
            when(attendanceRepository.findById(recordId)).thenReturn(Optional.of(record));
            var longMemo = "あ".repeat(301);

            // Act & Assert
            assertThatThrownBy(() -> service.updateMemo(recordId, employee.getId(), longMemo))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("300");
        }

        @Test
        @DisplayName("メモを空文字で更新するとnullになる")
        void updateMemo_emptyString_setsNull() {
            // Arrange
            var recordId = UUID.randomUUID();
            var record = AttendanceRecord.builder()
                    .id(recordId)
                    .employee(employee)
                    .workDate(TODAY_TOKYO)
                    .clockIn(FIXED_INSTANT)
                    .memo("以前のメモ")
                    .build();
            when(attendanceRepository.findById(recordId)).thenReturn(Optional.of(record));
            when(attendanceRepository.save(any(AttendanceRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            var result = service.updateMemo(recordId, employee.getId(), "");

            // Assert
            assertThat(result.memo()).isNull();
        }
    }

    @Nested
    @DisplayName("勤怠履歴でのメモ表示")
    class HistoryWithMemo {

        @Test
        @DisplayName("履歴レスポンスにメモが含まれる")
        void getHistory_recordWithMemo_includesMemoInResponse() {
            // Arrange
            var record = AttendanceRecord.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .workDate(TODAY_TOKYO)
                    .clockIn(FIXED_INSTANT)
                    .clockOut(FIXED_INSTANT.plusSeconds(28800))
                    .memo("客先直行")
                    .corrected(false)
                    .build();
            when(attendanceRepository.findByEmployeeIdAndWorkDateBetween(
                    employee.getId(),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 31)
            )).thenReturn(java.util.List.of(record));

            // Act
            var result = service.getHistory(employee.getId(), "2025-01");

            // Assert
            assertThat(result.days()).hasSize(1);
            assertThat(result.days().get(0).records().get(0).memo()).isEqualTo("客先直行");
        }
    }
}
