package com.wizlit.path.repository;

import com.wizlit.path.entity.Point;
import com.wizlit.path.model.domain.PointDto;

import io.micrometer.common.lang.Nullable;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.r2dbc.repository.Query;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.time.Instant;

@Repository
public interface PointRepository extends ReactiveCrudRepository<Point, Long> {
    // Mono<Boolean> existsByPointIdIn(Collection<Long> id);
    Flux<Point> findAllByPointUpdatedTimestampAfter(Long afterUpdateTimestamp);
    
    @Query("SELECT id FROM point")
    Flux<Long> findAllPointIds();
    
    @Query("SELECT * FROM point WHERE point_id = :id AND point_updated_timestamp > :updatedAfter")
    Mono<Point> findByIdAndPointUpdatedTimestampAfter(Long id, Instant updatedAfter);

    @Query("SELECT * FROM point WHERE point_id IN (:ids) AND point_updated_timestamp > :updatedAfter")
    Flux<Point> findAllByIdAndPointUpdatedTimestampAfter(Collection<Long> ids, Instant updatedAfter);

    // You can add custom query methods, e.g.,
    // Flux<User> findByName(String name);

    @Query("SELECT EXISTS(SELECT 1 FROM point WHERE point_id IN (:ids))")
    Mono<Boolean> existsByIdIn(Collection<Long> ids);
    

    /**
     * Fetch full point data including ordered memo IDs.
     * - Timestamps returned as epoch milliseconds (bigint).
     * - Filters by updatedAfter if provided; when null, returns all.
     */
    @Query(
      "SELECT " +
      "  p.point_id                                          AS point_id, " +
      "  p.point_title                                       AS point_title, " +
      "  p.point_created_user                                AS point_created_user, " +
      "  p.point_summary                                     AS point_summary, " +
      "  (EXTRACT(EPOCH FROM p.point_summary_timestamp) * 1000)::bigint AS point_summary_timestamp, " +
      "  (EXTRACT(EPOCH FROM p.point_created_timestamp) * 1000)::bigint AS point_created_timestamp, " +
      "  (EXTRACT(EPOCH FROM p.point_updated_timestamp) * 1000)::bigint AS point_updated_timestamp, " +
      "  COALESCE(ARRAY_AGG(pm.memo_id ORDER BY pm.memo_order), ARRAY[]::bigint[]) AS memo_ids_in_order " +
      "FROM point p " +
      "LEFT JOIN point_memo pm ON p.point_id = pm.point_id " +
      "WHERE p.point_id IN (:ids) " +
      "  AND (:updatedAfter IS NULL OR p.point_updated_timestamp > :updatedAfter) " +
      "GROUP BY p.point_id, p.point_title, p.point_created_user, p.point_summary, p.point_summary_timestamp, p.point_created_timestamp, p.point_updated_timestamp"
    )
    Flux<PointDto> findFullPointsByIds(
        @Param("ids") Collection<Long> ids,
        @Param("updatedAfter") @Nullable Instant updatedAfter
    );
}