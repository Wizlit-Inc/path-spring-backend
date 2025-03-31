package com.wizlit.path.service.impl;

import com.wizlit.path.entity.LastUpdate;
import com.wizlit.path.repository.LastUpdateRepository;
import com.wizlit.path.service.LastUpdateService;
import com.wizlit.path.utils.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LastUpdateServiceImpl implements LastUpdateService {

    /**
     * Service 규칙:
     * 1. 1개의 repository 만 정의
     * 2. repository 의 각 기능은 반드시 한 번만 호출
     * 3. repository 기능에는 .onErrorMap(error -> Validator.from(error).toException()) 필수
     */

    private final LastUpdateRepository repository;
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<LastUpdate> getLastUpdate(String id) {
        return _get(id);
    }

    @Override
    public Mono<Boolean> hasUpdate(String id, Instant timestamp) {
        return _get(id)
                .map(lastUpdate -> lastUpdate.getUpdated_time().isAfter(timestamp))
                .defaultIfEmpty(false);
    }

    private Mono<LastUpdate> _get(String id) {
        return repository.findById(id)
                .onErrorMap(error -> Validator.from(error)
                        .toException());
    }

    @Override
    public Mono<LastUpdate> update(String id) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setUpdated_time(Instant.now());
                    return repository.save(existing);
                })
                .switchIfEmpty(template.insert(LastUpdate.class)
                        .using(LastUpdate.builder().id(id).updated_time(Instant.now()).build()))
                .onErrorMap(error -> Validator.from(error)
                        .toException());
    }

}