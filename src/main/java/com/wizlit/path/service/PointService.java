package com.wizlit.path.service;

import com.wizlit.path.entity.Point;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface PointService {
    Mono<Tuple2<Long, Long>> convertPointsToLong(String originPointId, String destinationPointId);
    Mono<Point> findExistingPoint(Long id);
    Flux<Point> getAllPoints();
    Mono<Point> createPoint(Point point);
    Mono<Point> updatePoint(Point point);
    Mono<Boolean> validatePointsExist(Long... pointIds);
}
