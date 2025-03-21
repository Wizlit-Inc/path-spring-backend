package com.wizlit.path.repository;

import com.wizlit.path.entity.Point;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface PointRepository extends ReactiveCrudRepository<Point, Long> {
    Mono<Boolean> existsByIdIn(Collection<Long> id);
    // You can add custom query methods, e.g.,
    // Flux<User> findByName(String name);
}