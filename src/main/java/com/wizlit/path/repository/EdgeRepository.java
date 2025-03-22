package com.wizlit.path.repository;

import com.wizlit.path.entity.Edge;
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
    
    Mono<Edge> findByOriginPointAndDestinationPoint(Long originPoint, Long destinationPoint);

    @Query("WITH RECURSIVE paths AS (" +
            "  SELECT origin_point, destination_point, 1 AS depth FROM edge " +
            "  WHERE origin_point = :origin_point " +
            "  UNION ALL " +
            "  SELECT p.origin_point, e.destination_point, p.depth + 1 " +
            "  FROM paths p " +
            "  INNER JOIN edge e ON p.destination_point = e.origin_point " +
            "  WHERE p.depth < :depth" +
            ") " +
            "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END " +
            "FROM paths " +
            "WHERE destination_point = :destination_point")
    Mono<Boolean> existsPathWithinDepth(@Param("origin_point") Long originPoint, @Param("destination_point") Long destinationPoint, @Param("depth") int depth);

    @Query("SELECT * FROM edge WHERE origin_point IN (:points) OR destination_point IN (:points)")
    Flux<Edge> findAllByPointIdIn(@Param("points") Collection<Long> points);
}