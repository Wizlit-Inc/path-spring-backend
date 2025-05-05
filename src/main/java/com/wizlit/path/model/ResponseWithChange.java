package com.wizlit.path.model;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ResponseWithChange<T> {
    private final T data;
    private final Long lastChangeTime;
    
    public ResponseWithChange(T data) {
        this.data = data;
        this.lastChangeTime = Instant.now().toEpochMilli();
    }
    
    public ResponseWithChange(T data, Instant lastChangeTime) {
        this.data = data;
        this.lastChangeTime = lastChangeTime.toEpochMilli();
    }

    public ResponseEntity<ResponseWithChange<T>> toResponseEntity(HttpStatus status) {
        return ResponseEntity.status(status)
                .body(this);
    }
}
