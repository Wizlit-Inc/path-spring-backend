package com.wizlit.path.model;

import java.time.Instant;

import lombok.Builder;

@Builder
public record LastChange<T>(Instant lastChangeTime, T data) {
}
