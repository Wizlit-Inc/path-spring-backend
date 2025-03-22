package com.wizlit.path.service;

import com.wizlit.path.entity.Edge;
import com.wizlit.path.entity.Point;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EdgeService {

    Flux<Edge> getAllEdgesByPoints(List<Point> points);

    Mono<Edge> findExistingEdge(Long originPointId, Long destinationPointId);
    Mono<Edge> validateEdgeExists(Long originPointId, Long destinationPointId);
    Mono<Boolean> validateNotBackwardPath(Long originPointId, Long destinationPointId, int depth);

    Mono<Edge> createEdge(Edge newEdge);
    Flux<Edge> createEdge(Edge... newEdges);
    Mono<Edge> createEdge(Long originPointId, Long destinationPointId);

    Flux<Edge> splitEdge(Long originPointId, Long destinationPointId, Long middlePointId);

    Mono<Void> deleteEdge(Edge edgeToDelete);
    Mono<Void> deleteEdge(Long originPointId, Long destinationPointId);
}