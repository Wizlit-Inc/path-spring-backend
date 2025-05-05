package com.wizlit.path.service.impl;

import com.wizlit.path.entity.Point;
import com.wizlit.path.model.domain.EdgeDto;
import com.wizlit.path.model.domain.PointDto;
import com.wizlit.path.model.domain.UserDto;
import com.wizlit.path.service.PointService;
import com.wizlit.path.service.manager.EdgeManager;
import com.wizlit.path.service.manager.PointManager;
import com.wizlit.path.service.manager.ProjectManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    /**
     * Service 규칙:
     * 1. repository 직접 호출 X (helper 를 통해서만 호출 - 동일한 helper 가 다른 곳에서도 쓰여도 됨)
     */
    
    private final ProjectManager projectManager;
    private final PointManager pointManager;
    private final EdgeManager edgeManager;
    
    @Override
    public Flux<PointDto> listPointsByIds(List<Long> ids, Instant updatedAfter) {
        return pointManager.getFullPoints(ids, updatedAfter);
    }

    @Override
    public Mono<PointDto> getPoint(Long id, Instant updatedAfter) {
        return pointManager.getFullPoint(id, updatedAfter);
    }

    @Transactional
    @Override
    public Mono<PointDto> createPoint(
        Long projectId,
        UserDto user,
        String title,
        Long originPointId,
        Long destinationPointId
    ) {
        Point newPoint = Point.builder()
            .pointTitle(title)
            .pointCreatedUser(user.getUserId())
            .build();
        
        return projectManager.isProjectExists(projectId, true)
            .then(Mono.<Point>defer(() -> {
                if (originPointId == null && destinationPointId == null) {
                    // Only adding a new point with no connections
                    return pointManager.createPoint(newPoint);
                    
                } else if (originPointId == null || destinationPointId == null) {
                    // Connect point with one edge - validate existence of origin or destination
                    Long existingPointId = originPointId != null ? originPointId : destinationPointId;
                    return pointManager.validatePointsExist(existingPointId)
                        .then(pointManager.createPoint(newPoint))
                        .flatMap(savedPoint ->
                            edgeManager.createEdge(
                                originPointId != null ? originPointId : savedPoint.getPointId(),
                                destinationPointId != null ? destinationPointId : savedPoint.getPointId()
                            ).then(Mono.just(savedPoint))
                        );

                } else {
                    // Both origin and destination provided: split edge
                    return edgeManager.validateNoBackwardPath(originPointId, destinationPointId, 5)
                        .then(pointManager.createPoint(newPoint))
                        .flatMap(savedMiddlePoint ->
                            edgeManager.splitEdge(originPointId, destinationPointId, savedMiddlePoint.getPointId())
                                .then(Mono.just(savedMiddlePoint))
                        );
                }
            }))
            .flatMap(savedPoint -> 
                projectManager.addPointToProject(projectId, savedPoint.getPointId())
                    .then(Mono.just(PointDto.from(savedPoint, Collections.emptyList())))
            );
    }

    @Transactional
    @Override
    public Mono<EdgeDto> connectPoints(Long originPointId, Long destinationPointId) {
        return edgeManager.validateEdgeDoesNotExist(originPointId, destinationPointId)
            .then(pointManager.validatePointsExist(originPointId, destinationPointId))
            .then(edgeManager.validateNoBackwardPath(originPointId, destinationPointId, 5))
            .then(edgeManager.createEdge(originPointId, destinationPointId))
            .map(edge -> EdgeDto.fromEdge(edge));
    }

    @Transactional
    @Override
    public Mono<Void> disconnectPoints(Long originPointId, Long destinationPointId) {
        return edgeManager.deleteEdgeByPoints(originPointId, destinationPointId);
    }

    @Transactional
    @Override
    public Mono<PointDto> updatePoint(Long pointId, String title) {
        return pointManager.updatePoint(pointId, title, null)
            .flatMap(point -> pointManager.getFullPoint(point.getPointId(), null));
    }

    @Transactional
    @Override
    public Mono<Void> deletePoint(Long pointId) {
        return pointManager.deletePoint(pointId);
    }

    @Transactional
    @Override
    public Mono<Void> moveMemo(Long currentPointId, Long memoId, Long newPointId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'moveMemo'");
    }

    @Transactional
    @Override
    public Mono<PointDto> reorderMemo(Long pointId, Long memoId, Long newPosition) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reorderMemo'");
    }
    
}
