package com.wizlit.path.service.impl;

import com.wizlit.path.entity.Edge;
import com.wizlit.path.entity.Point;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.BusinessLogicException;
import com.wizlit.path.exception.ErrorCodes;
import com.wizlit.path.exception.ValidationException;
import com.wizlit.path.repository.EdgeRepository;
import com.wizlit.path.repository.PointRepository;
import com.wizlit.path.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final PointRepository pointRepository;
    private final EdgeRepository edgeRepository;

    @Override
    public Mono<Point> createPoint(Point point) {
        return pointRepository.save(point);
    }

    // get all points
    @Override
    public Mono<List<Point>> getAllPoints() {
        return pointRepository.findAll().collectList();
    }

    private static final String BACKWARD_PATH_EXISTS_ERROR = "A backward path exists from endPoint to startPoint within %d edges";

    @Override
    public Mono<Point> addMiddlePoint(String startPoint, String endPoint, Point middlePoint, int depth) {
        if (startPoint == null || endPoint == null) {
            throw new ValidationException(ErrorCodes.NULL_PARAMETERS, 0, startPoint, endPoint);
        }

        try {
            Long start = Long.valueOf(startPoint);
            Long end = Long.valueOf(endPoint);

            if (start.equals(end)) {
                throw new ValidationException(ErrorCodes.SAME_POINTS);
            }

            return validatePointsExist(start, end)
                    .then(checkForBackwardPath(end, start, depth))
                    .then(createPoint(middlePoint))
                    .flatMap(savedMiddlePoint -> handleEdges(start, end, savedMiddlePoint))
                    .onErrorMap(ex -> {
                        if (ex instanceof ApiException apiException) {
                            return apiException; // The GlobalExceptionHandler will manage this automatically
                        }
                        return new BusinessLogicException(
                                ErrorCodes.UNKNOWN_ERROR,
                                "Unexpected error during edge creation - " + ex.getMessage()
                        );
                    });
        } catch (NumberFormatException ex) {
            throw new ValidationException(ErrorCodes.INVALID_NUMERIC_IDS, 0, startPoint, endPoint);
//            return Mono.error(new IllegalArgumentException(ErrorCodes.INVALID_NUMERIC_IDS.getMessage()));
        }
    }

    private Mono<Void> validatePointsExist(Long startPointId, Long endPointId) {
        return pointRepository.existsByIdIn(List.of(startPointId, endPointId))
                .flatMap(exist -> {
                    if (!Boolean.TRUE.equals(exist)) {
                        throw new ValidationException(ErrorCodes.NON_EXISTENT_POINTS, 0, startPointId, endPointId);
//                        return Mono.error(new IllegalArgumentException(ErrorCodes.NON_EXISTENT_POINTS.getMessage()));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> checkForBackwardPath(Long startPointId, Long endPointId, int depth) {
        return edgeRepository.existsPathWithinDepth(startPointId, endPointId, depth)
                .flatMap(pathExists -> {
                    if (Boolean.TRUE.equals(pathExists)) {
                        throw new BusinessLogicException(ErrorCodes.BACKWARD_PATH, 0, 5);
//                        return Mono.error(new IllegalArgumentException(ErrorCodes.BACKWARD_PATH_EXISTS.getFormattedMessage(depth)));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Point> handleEdges(Long startPointId, Long endPointId, Point middlePoint) {
        Edge newEdgeToMiddle = Edge.builder()
                .startPoint(startPointId)
                .endPoint(middlePoint.getId())
                .build();

        Edge newEdgeFromMiddle = Edge.builder()
                .startPoint(middlePoint.getId())
                .endPoint(endPointId)
                .build();

        return edgeRepository.findByStartPointAndEndPoint(startPointId, endPointId)
                .flatMap(existingEdge -> edgeRepository.delete(existingEdge)
                        .then(saveEdgesAndReturnPoint(middlePoint, newEdgeToMiddle, newEdgeFromMiddle)))
                .switchIfEmpty(saveEdgesAndReturnPoint(middlePoint, newEdgeToMiddle, newEdgeFromMiddle));
    }

    private Mono<Point> saveEdgesAndReturnPoint(Point middlePoint, Edge edge1, Edge edge2) {
        return edgeRepository.saveAll(List.of(edge1, edge2))
                .then(Mono.just(middlePoint))
                .onErrorMap(error -> {
                    throw new BusinessLogicException(ErrorCodes.SAVE_FAILED, error.getMessage());
                });
    }

}
