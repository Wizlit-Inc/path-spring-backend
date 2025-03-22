package com.wizlit.path.service.impl;

import com.wizlit.path.entity.Point;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.repository.PointRepository;
import com.wizlit.path.service.PointService;
import com.wizlit.path.utils.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    /**
     * Service 규칙:
     * 1. 1개의 repository 만 정의
     * 2. repository 의 각 기능은 반드시 한 번만 호출
     * 3. repository 기능에는 .onErrorMap(error -> Validator.from(error).toException()) 필수
     */

    private final PointRepository pointRepository;

    @Override
    public Mono<Tuple2<Long, Long>> convertPointsToLong(String originPointId, String destinationPointId) {
        if (originPointId == null || destinationPointId == null) {
            return Mono.error(new ApiException(ErrorCode.NULL_POINTS, originPointId, destinationPointId));
        }

        if (originPointId.equals(destinationPointId)) {
            return Mono.error(new ApiException(ErrorCode.SAME_POINTS));
        }

        try {
            Long origin = Long.valueOf(originPointId);
            Long destination = Long.valueOf(destinationPointId);
            return Mono.just(Tuples.of(origin, destination));
        } catch (NumberFormatException ex) {
            return Mono.error(new ApiException(ErrorCode.INVALID_NUMERIC_IDS, originPointId, destinationPointId));
        }
    }

    @Override
    public Mono<Point> findExistingPoint(Long id) {
        return pointRepository.findById(id)
                .onErrorMap(error -> Validator.from(error)
                        .containsAllElseError(
                                new ApiException(ErrorCode.POINT_NOT_FOUND, id),
                                "point", "not", "found"
                        )
                        .toException())
                .switchIfEmpty(Mono.error(new ApiException(ErrorCode.POINT_NOT_FOUND, id)));
    }

    @Override
    public Flux<Point> getAllPoints() {
        return pointRepository.findAll()
                .onErrorMap(error -> Validator.from(error)
                        .toException());
    }

    @Override
    public Mono<Point> createPoint(Point point) {
        return _createPoint(point);
    }

    private Mono<Point> _createPoint(Point newPoint) {
        newPoint.setTitle(newPoint.getTitle().trim());
        if (newPoint.getObjective() != null) newPoint.setObjective(newPoint.getObjective().trim());
        if (newPoint.getDocument() != null) newPoint.setDocument(newPoint.getDocument().trim());

        return pointRepository.save(newPoint)
                .onErrorMap(error -> Validator.from(error)
                        .containsAllElseError(
                                new ApiException(ErrorCode.POINT_NAME_DUPLICATED, newPoint.getTitle()),
                                "unique", "key"
                        )
                        .toException());
    }

    // Helper method to check whether both points exist in the repository
    @Override
    public Mono<Boolean> validatePointsExist(Long... pointIds) {
        return pointRepository.existsByIdIn(List.of(pointIds))
                .onErrorMap(error -> Validator.from(error)
                        .toException())
                .flatMap(exists -> {
                    if (Boolean.FALSE.equals(exists)) {
                        return Mono.error(new ApiException(ErrorCode.NON_EXISTENT_POINTS, Arrays.toString(pointIds)));
                    }
                    return Mono.just(true);
                });
    }
}
