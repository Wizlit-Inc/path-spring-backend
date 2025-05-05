package com.wizlit.path.service;

import java.time.Instant;
import java.util.List;

import com.wizlit.path.model.domain.EdgeDto;
import com.wizlit.path.model.domain.PointDto;
import com.wizlit.path.model.domain.UserDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PointService {
    // Mono<Tuple2<Long, Long>> convertPointsToLong(String originPointId, String destinationPointId);
    // Mono<Point> findExistingPoint(Long id);
    // Flux<Point> listPointsIn(List<Long> ids, Map<Long, Instant> listOfUpdatedAfter);
    // Flux<PointDto> getAllPoints();
    // Flux<Long> getAllPointIds();
    // Mono<Boolean> validatePointsExist(Long... pointIds);
    // Flux<Long> findMemoIdsByPointId(Long pointId);
    Mono<PointDto> getPoint(Long id, Instant updatedAfter);
    Flux<PointDto> listPointsByIds(List<Long> ids, Instant updatedAfter);
    Mono<PointDto> createPoint(Long projectId, UserDto user, String title, Long originPointId, Long destinationPointId);
    Mono<EdgeDto> connectPoints(Long originPointId, Long destinationPointId);
    Mono<Void> disconnectPoints(Long originPointId, Long destinationPointId);
    Mono<PointDto> updatePoint(Long pointId, String title);
    Mono<Void> deletePoint(Long pointId);
    Mono<Void> moveMemo(Long currentPointId, Long memoId, Long newPointId);
    Mono<PointDto> reorderMemo(Long pointId, Long memoId, Long newPosition);
}
