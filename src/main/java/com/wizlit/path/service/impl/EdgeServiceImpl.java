package com.wizlit.path.service.impl;

import com.wizlit.path.entity.Edge;
import com.wizlit.path.entity.Point;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.BusinessLogicException;
import com.wizlit.path.exception.ErrorCodes;
import com.wizlit.path.exception.ValidationException;
import com.wizlit.path.repository.EdgeRepository;
import com.wizlit.path.repository.PointRepository;
import com.wizlit.path.service.EdgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EdgeServiceImpl implements EdgeService {

    private final EdgeRepository edgeRepository;
    private final PointRepository pointRepository;

    // get all edges that are related to input points
    @Override
    public Flux<Edge> getAllEdgesByPoints(List<Point> points) {
        List<Long> pointIds = points.stream()
                .map(Point::getId)
                .filter(Objects::nonNull)
                .toList();
        return edgeRepository.findAllByPointIdIn(pointIds);
    }


    /**
     * Checks whether a path exists between the specified start point and end point within a given depth.
     *
     * @param startPoint the starting point of the path, represented as a String
     * @param endPoint the ending point of the path, represented as a String
     * @param depth the maximum depth to check for a connection between the points
     * @return a {@code Mono<Boolean>} that emits {@code true} if a path exists within the given depth, or {@code false} otherwise
     */
    @Override
    public Mono<Boolean> isPathWithinDepth(String startPoint, String endPoint, int depth) {
        Long start = Long.valueOf(startPoint);
        Long end = Long.valueOf(endPoint);
        return edgeRepository.existsPathWithinDepth(start, end, depth);
    }

    /**
     * Adds multiple edges to the repository and returns a Flux containing the saved edges.
     *
     * @param edges the edges to be added
     * @return a {@code Flux<Edge>} containing the saved edges
     */
    private Flux<Edge> _addEdges(Edge... edges) {
        return edgeRepository.saveAll(Flux.just(edges));
    }

    /**
     * Establishes a connection between two points by creating an edge.
     * Ensures there is no backward path within the defined depth before creating the edge.
     * Also checks if both startPoint and endPoint exist in the repository.
     *
     * @param startPoint the starting point of the edge
     * @param endPoint   the ending point of the edge
     * @param depth      the maximum depth to check for potential backward paths
     * @return a {@code Mono<Edge>} containing the created edge, or an error if a backward path exists or points are missing
     */
    @Override
    public Mono<Edge> connectEdge(String startPoint, String endPoint, int depth) {
        return validateInputPoints(startPoint, endPoint)
                .flatMap(tuple -> {
                    Long start = tuple.getT1();
                    Long end = tuple.getT2();
                    return validateEdgeExists(start, end)
                            .then(validatePointsExist(start, end))
                            .then(validateNoBackwardPath(end, start, depth))
                            .then(createEdge(start, end));
                })
                .onErrorMap(ex -> {
                    if (ex instanceof ApiException apiException) {
                        return apiException; // The GlobalExceptionHandler will manage this automatically
                    }
                    return new BusinessLogicException(
                            ErrorCodes.UNKNOWN_ERROR,
                            "Unexpected error during edge creation - " + ex.getMessage()
                    );
                });
    }

    // Helper method to validate string input points and convert them to Long
    private Mono<Tuple2<Long, Long>> validateInputPoints(String startPoint, String endPoint) {
        if (startPoint == null || endPoint == null) {
            throw new ValidationException(ErrorCodes.NULL_PARAMETERS, 0, startPoint, endPoint);
//            return Mono.error(new IllegalArgumentException(ErrorCodes.NULL_PARAMETERS.getMessage()));
        }

        if (startPoint.equals(endPoint)) {
            throw new ValidationException(ErrorCodes.SAME_POINTS);
        }

        try {
            Long start = Long.valueOf(startPoint);
            Long end = Long.valueOf(endPoint);
            return Mono.just(Tuples.of(start, end));
        } catch (NumberFormatException ex) {
            throw new ValidationException(ErrorCodes.INVALID_NUMERIC_IDS, 0, startPoint, endPoint);
//            return Mono.error(new IllegalArgumentException(ErrorCodes.INVALID_NUMERIC_IDS.getMessage()));
        }
    }

    private Mono<Boolean> validateEdgeExists(Long start, Long end) {
        return edgeRepository.findByStartPointAndEndPoint(start, end)
                .flatMap(exists -> {
                    if (exists != null) {
                        throw new ValidationException(ErrorCodes.EDGE_ALREADY_EXISTS, 0, start, end);
//                        return Mono.error(new IllegalArgumentException(ErrorCodes.EDGE_ALREADY_EXISTS.getMessage()));
                    }
                    return Mono.just(true);
                });
    }

    // Helper method to check whether both points exist in the repository
    private Mono<Boolean> validatePointsExist(Long start, Long end) {
        return pointRepository.existsByIdIn(List.of(start, end))
                .flatMap(exists -> {
                    if (Boolean.FALSE.equals(exists)) {
                        throw new ValidationException(ErrorCodes.NON_EXISTENT_POINTS, 0, start, end);
//                        return Mono.error(new IllegalArgumentException(ErrorCatalog.NON_EXISTENT_POINTS.getMessage()));
                    }
                    return Mono.just(true);
                });
    }

    // Helper method to check for backward paths between the points
    private Mono<Boolean> validateNoBackwardPath(Long start, Long end, int depth) {
        return edgeRepository.existsPathWithinDepth(start, end, depth)
                .flatMap(backwardPathExists -> {
                    if (Boolean.TRUE.equals(backwardPathExists)) {
                        throw new BusinessLogicException(ErrorCodes.BACKWARD_PATH, 0, 5);
//                        return Mono.error(new IllegalArgumentException(ErrorCodes.BACKWARD_PATH_EXISTS.getFormattedMessage(depth)));
                    }
                    return Mono.just(true);
                });
    }

    // Helper method to create and save a new edge
    private Mono<Edge> createEdge(Long start, Long end) {
        Edge newEdge = Edge.builder()
                .startPoint(start)
                .endPoint(end)
                .build();
        return edgeRepository.save(newEdge)
                .onErrorMap(error -> {
                    throw new BusinessLogicException(ErrorCodes.SAVE_FAILED, error.getMessage());
                });
    }
}