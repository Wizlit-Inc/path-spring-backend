package com.wizlit.path.repository;

import com.wizlit.path.entity.FileMemo;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileMemoRepository extends R2dbcRepository<FileMemo, FileMemo.FileMemoId> {
    @Query("SELECT memo_id FROM file_memo WHERE file_id = :fileId")
    Flux<Long> findMemoIdsByFileId(Long fileId);

    @Query("SELECT * FROM file_memo WHERE file_id = :fileId")
    Flux<FileMemo> findByFileId(Long fileId);

    @Query("SELECT * FROM file_memo WHERE memo_id = :memoId")
    Flux<FileMemo> findByMemoId(Long memoId);

    @Query("DELETE FROM file_memo WHERE file_id = :fileId")
    Mono<Void> deleteByFileId(Long fileId);

    @Query("DELETE FROM file_memo WHERE memo_id = :memoId")
    Mono<Void> deleteByMemoId(Long memoId);
} 