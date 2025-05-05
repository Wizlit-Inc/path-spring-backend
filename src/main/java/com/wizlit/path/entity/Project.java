package com.wizlit.path.entity;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("project")
public class Project {
    @Id
    @Column("project_id")
    private Long projectId;

    @Column("project_created_user")
    private Long projectCreatedUser;

    @Column("project_created_timestamp")
    private Instant projectCreatedTimestamp;

    @Column("project_updated_timestamp")
    private Instant projectUpdatedTimestamp; // Project updated, ProjectPoint updated
    // Transient field to store point IDs
    @Transient
    private List<Long> pointIds;

    public List<Long> getPointIds() {
        return pointIds;
    }

    public void setPointIds(List<Long> pointIds) {
        this.pointIds = pointIds;
    }
}