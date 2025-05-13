package com.wizlit.path.repository;

import com.wizlit.path.entity.Project;
import com.wizlit.path.model.domain.ProjectDto;

import io.micrometer.common.lang.Nullable;

import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.r2dbc.repository.Query;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;

@Repository
public interface ProjectRepository extends ReactiveCrudRepository<Project, Long> {
    @Query("SELECT project_id, project_created_user, project_created_timestamp, project_updated_timestamp FROM project WHERE project_id = :projectId AND project_updated_timestamp > :updatedAfter")
    Mono<Project> findByProjectIdAndProjectUpdatedTimestampAfter(Long projectId, Instant updatedAfter);

    /**
     * Fetch full project data including associated point IDs.
     * - Timestamps returned as epoch milliseconds (bigint).
     * - Filters by updatedAfter if provided; when null, returns all.
     */
    @Query(
      "SELECT " +
      "  p.project_id                                           AS project_id, " +
      "  p.project_created_user                                 AS project_created_user, " +
      "  (EXTRACT(EPOCH FROM p.project_created_timestamp) * 1000)::bigint AS project_created_timestamp, " +
      "  (EXTRACT(EPOCH FROM p.project_updated_timestamp) * 1000)::bigint AS project_updated_timestamp, " +
      "  COALESCE(ARRAY_AGG(pp.point_id ORDER BY pp.point_id) FILTER (WHERE pp.point_id IS NOT NULL), ARRAY[]::bigint[]) AS point_ids " +
      "FROM project p " +
      "LEFT JOIN project_point pp ON p.project_id = pp.project_id " +
      "WHERE p.project_id IN (:ids) " +
      "  AND (:updatedAfter IS NULL OR p.project_updated_timestamp > :updatedAfter) " +
      "GROUP BY p.project_id, p.project_created_user, p.project_created_timestamp, p.project_updated_timestamp"
    )
    Flux<ProjectDto> findFullProjectsByIds(
        @Param("ids") Collection<Long> ids,
        @Param("updatedAfter") @Nullable Instant updatedAfter
    );
} 