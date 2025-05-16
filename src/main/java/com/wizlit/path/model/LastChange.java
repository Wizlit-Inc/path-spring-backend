package com.wizlit.path.model;

import java.time.Instant;

public record LastChange<T>(Instant lastChangeTime, T data) {
}
