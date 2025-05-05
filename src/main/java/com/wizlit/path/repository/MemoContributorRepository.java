package com.wizlit.path.repository;

import com.wizlit.path.entity.MemoContributor;
import com.wizlit.path.entity.MemoContributor.MemoContributorId;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MemoContributorRepository extends ReactiveCrudRepository<MemoContributor, MemoContributorId> {
    @Query("SELECT * FROM memo_contributor WHERE memo_id = :memoId AND user_id = :userId")
    Mono<MemoContributor> findByMemoIdAndUserId(Long memoId, Long userId);
} 