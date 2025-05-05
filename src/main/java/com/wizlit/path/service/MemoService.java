package com.wizlit.path.service;

import java.time.Instant;

import com.wizlit.path.model.domain.UserDto;
import com.wizlit.path.model.domain.MemoDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MemoService {
    Flux<MemoDto> listMemosByPointId(Long pointId, Instant updatedAfter);
    Mono<MemoDto> getMemo(Long memoId, Instant updatedAfter);
    Mono<MemoDto> createMemo(Long pointId, UserDto user, String title, String content);
    Mono<MemoDto> updateMemo(Long memoId, UserDto user, String title, String content, String reserveCode);
    Mono<MemoDto> updateExternalMemo(Long memoId, String externalKey, String title);
    Mono<String> reserveMemo(Long memoId, UserDto user, String currentReserveCode);
    Mono<Void> cancelReserve(Long memoId, UserDto user, String reserveCode);
    Mono<Void> moveMemo(Long memoId, Long newPointId);
}
