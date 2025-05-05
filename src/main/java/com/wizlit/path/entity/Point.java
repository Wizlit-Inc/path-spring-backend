package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("point")
public class Point {
    @Id
    @Column("point_id") 
    private Long pointId;

    @Column("point_title")
    private String pointTitle;

    @Column("point_created_user")
    private Long pointCreatedUser;

    @Column("point_summary")
    private String pointSummary; // ai summary of all memo summary. update on memoSummaryTimestamp n days ago

    @Column("point_summary_timestamp")
    private Instant pointSummaryTimestamp; // if memo update was happened because of summary, update this together

    @Column("point_created_timestamp")
    private Instant pointCreatedTimestamp;

    @Column("point_updated_timestamp")
    private Instant pointUpdatedTimestamp; // Point updated, PointMemo updated

    // Transient field to store memo IDs in order
    @Transient
    private List<Long> memoIdsInOrder;

    public List<Long> getMemoIdsInOrder() {
        return memoIdsInOrder;
    }

    public void setMemoIdsInOrder(List<Long> memoIdsInOrder) {
        this.memoIdsInOrder = memoIdsInOrder;
    }
}