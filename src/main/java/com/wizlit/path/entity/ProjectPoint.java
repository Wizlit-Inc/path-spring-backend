package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("project_point")
public class ProjectPoint {
    @Column("project_id")
    private Long projectId;

    @Column("point_id")
    private Long pointId;

    // Composite primary key
    public static class ProjectPointId {
        private Long projectId;
        private Long pointId;
    }
} 