package com.wizlit.path.service;

import java.time.Instant;

import com.wizlit.path.model.domain.UserDto;
import com.wizlit.path.model.domain.MemoDto;
import com.wizlit.path.model.domain.ReserveMemoDto;
import com.wizlit.path.model.domain.MemoRevisionDto;
import com.wizlit.path.model.domain.RevisionContentDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MemoService {
    Flux<MemoDto> listMemosByPointId(Long pointId, Instant updatedAfter);
    Mono<MemoDto> getMemo(Long memoId, Instant updatedAfter);
    Mono<MemoDto> createEmbedMemo(Long pointId, UserDto user, String title, String embedContent);
    Mono<MemoDto> createMemo(Long pointId, UserDto user, String title, String content);
    Mono<MemoDto> updateMemo(Long memoId, UserDto user, String content, String reserveCode);
    Mono<MemoDto> updateEmbedContent(Long memoId, String embedContent);
    Mono<MemoDto> updateTitle(Long memoId, String title);
    Mono<ReserveMemoDto> reserveMemo(Long memoId, UserDto user, String currentReserveCode);
    Mono<Void> cancelReserve(Long memoId, UserDto user, String reserveCode);
    Mono<Void> moveMemo(Long memoId, Long newPointId);
    Flux<MemoRevisionDto> listRevisions(Long memoId, Instant beforeTimestamp, int limit);
    Mono<RevisionContentDto> getRevisionContent(Long revisionContentId);
    Mono<Void> rollbackToRevision(Long memoId, UserDto user, Long revisionId, Boolean forced);
}
