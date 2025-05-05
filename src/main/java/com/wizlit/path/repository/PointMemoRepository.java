package com.wizlit.path.repository;

import com.wizlit.path.entity.PointMemo;

import java.util.List;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PointMemoRepository extends ReactiveCrudRepository<PointMemo, Long> {
    @Query("SELECT memo_id FROM point_memo WHERE point_id = :pointId ORDER BY memo_order")
    Flux<Long> findMemoIdsByPointId(Long pointId);
    
    // get all pointMemo (do not need to order) -> input: list of pointIds
    @Query("SELECT * FROM point_memo WHERE point_id IN (:pointIds)")
    Flux<PointMemo> findAllByPointIdIn(List<Long> pointIds);

    @Query("SELECT * FROM point_memo WHERE memo_id = :memoId")
    Mono<PointMemo> findByMemoId(Long memoId);

    @Query("DELETE FROM point_memo WHERE point_id = :pointId")
    Mono<Void> deleteByPointId(Long pointId);

    @Query("DELETE FROM point_memo WHERE memo_id = :memoId")
    Mono<Void> deleteByMemoId(Long memoId);

    @Query("SELECT MAX(memo_order) FROM point_memo WHERE point_id = :pointId")
    Mono<Integer> findMaxMemoOrderByPointId(Long pointId);

    @Query("SELECT * FROM point_memo WHERE point_id = :pointId AND memo_order > :memoOrder ORDER BY memo_order")
    Flux<PointMemo> findByPointIdAndMemoOrderGreaterThan(Long pointId, Integer memoOrder);

    @Query("SELECT * FROM point_memo WHERE point_id = :pointId ORDER BY memo_order")
    Flux<PointMemo> findAllByPointIdOrderByMemoOrder(Long pointId);
    
    @Query("SELECT COUNT(*) FROM point_memo WHERE point_id = :pointId")
    Mono<Integer> countByPointId(Long pointId);

    @Query("SELECT * FROM point_memo WHERE point_id = :pointId AND memo_id = :memoId")
    Mono<PointMemo> findByPointIdAndMemoId(Long pointId, Long memoId);
} 