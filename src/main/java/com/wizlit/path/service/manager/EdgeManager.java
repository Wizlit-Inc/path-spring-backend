package com.wizlit.path.service.manager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.wizlit.path.entity.Edge;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.repository.EdgeRepository;
import com.wizlit.path.utils.Validator;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * EdgeManager is a component responsible for managing edge-related operations in a graph structure.
 * It handles edge creation, deletion, validation, and path checking in a reactive manner.
 * 
 * This manager follows strict rules for repository usage and error handling:
 * 1. Each repository should be used exclusively within this manager
 * 2. Repository operations should be called exactly once per method
 * 3. All repository operations must include error mapping using Validator
 * 4. No direct calls to other helpers or services are allowed
 * 5. Methods should not return DTOs directly
 * 
 * Manager 규칙:
 * 1. 동일한 repository 가 다른 곳에서도 쓰이면 안됨
 * 2. repository 의 각 기능은 반드시 한 번만 호출
 * 3. repository 기능에는 .onErrorMap(error -> Validator.from(error).toException()) 필수
 * 4. 다른 helper 나 service 호출 금지
 * 5. DTO 반환 금지
 */
@Component
@RequiredArgsConstructor
public class EdgeManager {

    private final EdgeRepository edgeRepository;

    /**
     * Finds all edges that are connected to any of the specified points.
     *
     * @param pointIds List of point IDs to find connected edges for
     * @return A Flux of edges connected to the specified points
     */
    public Flux<Edge> findEdgesByPointIds(List<Long> pointIds) {
        return edgeRepository.findAllByPointIdIn(pointIds)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.NON_EXISTENT_POINTS, pointIds),
                    "foreign", "key", "edge"
                )
                .toException());
    }

    /**
     * Validates edge existence and optionally throws an exception.
     *
     * @param originPointId The ID of the origin point
     * @param destinationPointId The ID of the destination point
     * @param throwException Whether to throw an exception if the edge exists
     * @return A Mono containing the edge if found, or empty if not found
     * @throws ApiException with ErrorCode.EDGE_ALREADY_EXISTS if throwException is true and edge exists
     */
    private Mono<Edge> _validateEdgeExistence(Long originPointId, Long destinationPointId, Boolean throwException) {
        return edgeRepository.findByOriginPointAndDestinationPoint(originPointId, destinationPointId)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.NON_EXISTENT_POINTS, List.of(originPointId, destinationPointId)),
                    "foreign", "key", "edge"
                )
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

    public Mono<Edge> findEdge(Long originPointId, Long destinationPointId) {
        return _validateEdgeExistence(originPointId, destinationPointId, false);
    }

    public Mono<Edge> validateEdgeDoesNotExist(Long originPointId, Long destinationPointId) {
        return _validateEdgeExistence(originPointId, destinationPointId, true);
    }

    /**
     * Validates that there is no backward path between two points within a specified depth.
     *
     * @param originPointId The ID of the origin point
     * @param destinationPointId The ID of the destination point
     * @param depth The maximum depth to check for backward paths
     * @return A Mono containing true if no backward path exists
     * @throws ApiException with ErrorCode.BACKWARD_PATH if a backward path is found
     */
    public Mono<Boolean> validateNoBackwardPath(Long originPointId, Long destinationPointId, int depth) {
        return edgeRepository.existsPathWithinDepth(destinationPointId, originPointId, depth)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.NON_EXISTENT_POINTS, List.of(originPointId, destinationPointId)),
                    "foreign", "key", "edge"
                )
                .toException())
            .flatMap(backwardPathExists -> {
                if (Boolean.TRUE.equals(backwardPathExists)) {
                    return Mono.error(new ApiException(ErrorCode.BACKWARD_PATH, depth, originPointId, destinationPointId));
                }
                return Mono.just(true);
            });
    }

    /**
     * Saves a single edge to the repository.
     *
     * @param newEdge The edge to save
     * @return A Mono containing the saved Edge
     * @throws ApiException with ErrorCode.NON_EXISTENT_POINTS if either point doesn't exist
     */
    private Mono<Edge> _saveEdge(Edge newEdge) {
        return edgeRepository.save(newEdge)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.NON_EXISTENT_POINTS, List.of(newEdge.getOriginPoint(), newEdge.getDestinationPoint())),
                    "foreign", "key", "edge"
                )
                .toException());
    }

    public Mono<Edge> createEdge(Edge newEdge) {
        return _saveEdge(newEdge);
    }

    public Mono<Edge> createEdge(Long originPointId, Long destinationPointId) {
        Edge newEdge = Edge.builder()
            .originPoint(originPointId)
            .destinationPoint(destinationPointId)
            .build();
        return _saveEdge(newEdge);
    }

    /**
     * Saves multiple edges to the repository.
     *
     * @param newEdges The edges to save
     * @return A Flux of saved edges
     */
    private Flux<Edge> _saveEdges(Edge... newEdges) {
        return edgeRepository.saveAll(List.of(newEdges))
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.NON_EXISTENT_POINTS, Arrays.stream(newEdges)
                        .map(edge -> List.of(edge.getOriginPoint(), edge.getDestinationPoint()))
                        .flatMap(List::stream)
                        .distinct()
                        .collect(Collectors.toList())),
                    "foreign", "key", "edge"
                )
                .toException());
    }

    public Flux<Edge> createEdges(Edge... newEdges) {
        return _saveEdges(newEdges);
    }

    /**
     * Splits an existing edge by inserting a middle point, creating two new edges.
     *
     * @param originPointId The ID of the origin point
     * @param destinationPointId The ID of the destination point
     * @param middlePointId The ID of the point to insert in the middle
     * @return A Flux containing the two new edges
     */
    public Flux<Edge> splitEdge(Long originPointId, Long destinationPointId, Long middlePointId) {
        Edge toMiddle = Edge.builder()
            .originPoint(originPointId)
            .destinationPoint(middlePointId)
            .build();

        Edge fromMiddle = Edge.builder()
            .originPoint(middlePointId)
            .destinationPoint(destinationPointId)
            .build();

        return _validateEdgeExistence(originPointId, destinationPointId, false)
            .flatMapMany(this::deleteEdge)
            .switchIfEmpty(Flux.empty())
            .thenMany(_saveEdges(toMiddle, fromMiddle));
    }
    
    /**
     * Deletes an edge between two points.
     *
     * @param originPointId The ID of the origin point
     * @param destinationPointId The ID of the destination point
     * @return A Mono that completes when the edge is deleted
     */
    public Mono<Void> deleteEdgeByPoints(Long originPointId, Long destinationPointId) {
        return edgeRepository.deleteByOriginPointAndDestinationPoint(originPointId, destinationPointId)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.NON_EXISTENT_POINTS, List.of(originPointId, destinationPointId)),
                    "foreign", "key", "edge"
                )
                .toException());
    }

    public Mono<Void> deleteEdge(Edge edgeToDelete) {
        return deleteEdgeByPoints(edgeToDelete.getOriginPoint(), edgeToDelete.getDestinationPoint());
    }
}