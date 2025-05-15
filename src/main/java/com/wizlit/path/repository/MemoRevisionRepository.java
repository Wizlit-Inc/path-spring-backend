package com.wizlit.path.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import com.wizlit.path.entity.MemoRevision;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.time.Instant;

public interface MemoRevisionRepository extends R2dbcRepository<MemoRevision, Long> {
    @Query("SELECT * FROM memo_revision WHERE rev_memo = :revMemo ORDER BY rev_timestamp DESC LIMIT 1")
    Mono<MemoRevision> findLatestByRevMemo(Long revMemo);

    @Query("SELECT * FROM memo_revision WHERE rev_memo = :revMemo AND rev_timestamp < :beforeTimestamp ORDER BY rev_timestamp DESC LIMIT :limit")
    Flux<MemoRevision> findAllByRevMemoAndTimestampBefore(Long revMemo, Instant beforeTimestamp, int limit);

    @Query("SELECT * FROM memo_revision WHERE rev_memo = :revMemo ORDER BY rev_timestamp DESC LIMIT :limit")
    Flux<MemoRevision> findAllByRevMemo(Long revMemo, int limit);
}
