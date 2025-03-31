package com.wizlit.path.service;

import com.wizlit.path.entity.LastUpdate;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface LastUpdateService {
    Mono<LastUpdate> getLastUpdate(String id);
    Mono<Boolean> hasUpdate(String id, Instant timestamp);
    Mono<LastUpdate> update(String id);
}