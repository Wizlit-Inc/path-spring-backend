package com.wizlit.path.service.impl;

import com.wizlit.path.entity.Edge;
import com.wizlit.path.entity.Point;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.repository.EdgeRepository;
import com.wizlit.path.service.EdgeService;
import com.wizlit.path.utils.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EdgeServiceImpl implements EdgeService {

    /**
     * Service 규칙:
     * 1. 1개의 repository 만 정의
     * 2. repository 의 각 기능은 반드시 한 번만 호출
     * 3. repository 기능에는 .onErrorMap(error -> Validator.from(error).toException()) 필수
     */

    private final EdgeRepository edgeRepository;

    // get all edges that are related to input points
    @Override
    public Flux<Edge> getAllEdgesByPoints(List<Point> points) {
        List<Long> pointIds = points.stream()
                .map(Point::getId)
                .filter(Objects::nonNull)
                .toList();
        return edgeRepository.findAllByPointIdIn(pointIds)
                .onErrorMap(error -> Validator.from(error)
                        .toException());
    }

    @Override
    public Mono<Edge> findExistingEdge(Long originPointId, Long destinationPointId) {
        return _validateOrGetEdgeExists(originPointId, destinationPointId, false);
    }

    @Override
    public Mono<Edge> validateEdgeExists(Long originPointId, Long destinationPointId) {
        return _validateOrGetEdgeExists(originPointId, destinationPointId, true);
    }

    private Mono<Edge> _validateOrGetEdgeExists(Long originPointId, Long destinationPointId, Boolean throwException) {
        return edgeRepository.findByOriginPointAndDestinationPoint(originPointId, destinationPointId)
                .onErrorMap(error -> Validator.from(error)
                        .toException())
                .flatMap(existingEdge -> {
                    if (existingEdge != null) {
                        if (throwException) {
                            return Mono.error(new ApiException(ErrorCode.EDGE_ALREADY_EXISTS, originPointId, destinationPointId));
                        } else {
                            return Mono.just(existingEdge);
                        }
                    }
                    return Mono.empty();
                });
    }

    // Helper method to check for backward paths between the points
    @Override
    public Mono<Boolean> validateNotBackwardPath(Long originPointId, Long destinationPointId, int depth) {
        return edgeRepository.existsPathWithinDepth(destinationPointId, originPointId, depth)
                .onErrorMap(error -> Validator.from(error)
                        .toException())
                .flatMap(backwardPathExists -> {
                    if (Boolean.TRUE.equals(backwardPathExists)) {
                        return Mono.error(new ApiException(ErrorCode.BACKWARD_PATH, depth, originPointId, destinationPointId));
                    }
                    return Mono.just(true);
                });
    }

    @Override
    public Mono<Edge> createEdge(Edge newEdge) {
        return _createEdge(newEdge);
    }

    @Override
    public Flux<Edge> createEdge(Edge... newEdges) {
        return _createEdge(newEdges);
    }

    // Helper method to create and save a new edge
    @Override
    public Mono<Edge> createEdge(Long originPointId, Long destinationPointId) {
        Edge newEdge = Edge.builder()
                .originPoint(originPointId)
                .destinationPoint(destinationPointId)
                .build();
        return _createEdge(newEdge);
    }

    // Helper method to create and save a new edge
    @Override
    public Flux<Edge> splitEdge(Long originPointId, Long destinationPointId, Long middlePointId) {
        Edge toMiddle = Edge.builder()
                .originPoint(originPointId)
                .destinationPoint(middlePointId)
                .build();

        Edge fromMiddle = Edge.builder()
                .originPoint(middlePointId)
                .destinationPoint(destinationPointId)
                .build();

        return _validateOrGetEdgeExists(originPointId, destinationPointId, false)
                .flatMapMany(this::_deleteEdge)
                .switchIfEmpty(Flux.empty())
                .thenMany(_createEdge(toMiddle, fromMiddle))
                .thenMany(Flux.just(toMiddle, fromMiddle));
    }

    private Mono<Edge> _createEdge(Edge newEdge) {
        return edgeRepository.save(newEdge)
                .onErrorMap(error -> Validator.from(error)
                        .toException());
    }

    private Flux<Edge> _createEdge(Edge... newEdges) {
        return edgeRepository.saveAll(List.of(newEdges))
                .onErrorMap(error -> Validator.from(error)
                        .toException());
    }

    // Helper method to create and save a new edge
    @Override
    public Mono<Void> deleteEdge(Edge edgeToDelete) {
        return _deleteEdge(edgeToDelete);
    }

    // Helper method to create and save a new edge
    @Override
    public Mono<Void> deleteEdge(Long originPointId, Long destinationPointId) {
        Edge targetEdge = Edge.builder()
                .originPoint(originPointId)
                .destinationPoint(destinationPointId)
                .build();
        return _deleteEdge(targetEdge);
    }

    private Mono<Void> _deleteEdge(Edge edge) {
        return edgeRepository.delete(edge)
                .onErrorMap(error -> Validator.from(error)
                        .toException());
    }
}