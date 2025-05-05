package com.wizlit.path.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.wizlit.path.entity.Project;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectDto {
    private Long projectId;
    private Long projectCreatedUser;
    private Long projectCreatedTimestamp;
    private Long projectUpdatedTimestamp;
    private List<Long> pointIds;
    private List <Long> allPointIds;
    private List<EdgeDto> edges;

    // function: convert from Edge and Point using OutputPointDto>fromPoint, OutputEdgeDto>fromPoint
    public static ProjectDto from(Project project, List<Long> pointIds, List<Long> allPointIds, List<EdgeDto> edges) {
        return ProjectDto.builder()
                .projectId(project.getProjectId())
                .projectCreatedUser(project.getProjectCreatedUser())
                .projectCreatedTimestamp(project.getProjectCreatedTimestamp() != null ? project.getProjectCreatedTimestamp().toEpochMilli() : null)
                .projectUpdatedTimestamp(project.getProjectUpdatedTimestamp() != null ? project.getProjectUpdatedTimestamp().toEpochMilli() : null)
                .pointIds(pointIds)
                .allPointIds(allPointIds)
                .edges(edges)
                .build();
    }

    public ProjectDto append(List<Long> allPointIds, List<EdgeDto> edges) {
        this.allPointIds = allPointIds;
        this.edges = edges;
        return this;
    }

}
