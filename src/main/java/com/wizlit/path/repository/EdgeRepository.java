package com.wizlit.path.repository;

import com.wizlit.path.entity.Edge;
import com.wizlit.path.entity.EdgePK;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface EdgeRepository extends ReactiveCrudRepository<Edge, Long> {
    // You can add custom query methods, e.g.,
    // Flux<User> findByName(String name);
    
    Mono<Edge> findByStartPointAndEndPoint(Long startPoint, Long endPoint);

    @Query("WITH RECURSIVE paths AS (" +
            "  SELECT start_point, end_point, 1 AS depth FROM edge " +
            "  WHERE start_point = :start_point " +
            "  UNION ALL " +
            "  SELECT p.start_point, e.end_point, p.depth + 1 " +
            "  FROM paths p " +
            "  INNER JOIN edge e ON p.end_point = e.start_point " +
            "  WHERE p.depth < :depth" +
            ") " +
            "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END " +
            "FROM paths " +
            "WHERE end_point = :end_point")
    Mono<Boolean> existsPathWithinDepth(@Param("start_point") Long startPoint, @Param("end_point") Long endPoint, @Param("depth") int depth);

    @Query("SELECT * FROM edge WHERE start_point IN (:points) OR end_point IN (:points)")
    Flux<Edge> findAllByPointIdIn(@Param("points") Collection<Long> points);
}