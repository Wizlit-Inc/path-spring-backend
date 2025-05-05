package com.wizlit.path.repository;

import com.wizlit.path.entity.ProjectPoint;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProjectPointRepository extends ReactiveCrudRepository<ProjectPoint, ProjectPoint.ProjectPointId> {
    @Query("SELECT point_id FROM project_point WHERE project_id = :projectId")
    Flux<Long> findPointIdsByProjectId(Long projectId);
    
    @Query("SELECT * FROM project_point WHERE project_id = :projectId")
    Flux<ProjectPoint> findAllByProjectId(Long projectId);
    
    @Query("SELECT * FROM project_point WHERE point_id = :pointId")
    Flux<ProjectPoint> findAllByPointId(Long pointId);
    
    @Query("DELETE FROM project_point WHERE project_id = :projectId")
    Mono<Void> deleteByProjectId(Long projectId);
    
    @Query("DELETE FROM project_point WHERE point_id = :pointId")
    Mono<Void> deleteByPointId(Long pointId);
} 