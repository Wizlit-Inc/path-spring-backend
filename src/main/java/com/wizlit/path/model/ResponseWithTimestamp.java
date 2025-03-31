package com.wizlit.path.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Builder
public class ResponseWithTimestamp<T> {
    private final Long serverTime = Instant.now().toEpochMilli();
    private final T data;
}
