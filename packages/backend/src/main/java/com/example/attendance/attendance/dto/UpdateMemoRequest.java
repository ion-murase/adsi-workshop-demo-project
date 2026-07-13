package com.example.attendance.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateMemoRequest(
    @NotNull UUID attendanceRecordId,
    @Size(max = 300) String memo
) {
}
