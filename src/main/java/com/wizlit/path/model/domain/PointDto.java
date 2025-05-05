package com.wizlit.path.model.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wizlit.path.entity.Point;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointDto {
    private Long pointId;
    private String pointTitle;
    private Long pointCreatedUser;
    private String pointSummary;
    private Long pointSummaryTimestamp;
    private Long pointCreatedTimestamp;
    private Long pointUpdatedTimestamp;
    private List<Long> memoIdsInOrder;

    public static PointDto from(Point point, List<Long> memoIdsInOrder) {
        return PointDto.builder()
                .pointId(point.getPointId())
                .pointTitle(point.getPointTitle())
                .pointCreatedUser(point.getPointCreatedUser())
                .pointSummary(point.getPointSummary())
                .pointSummaryTimestamp(point.getPointSummaryTimestamp() != null ? point.getPointSummaryTimestamp().toEpochMilli() : null)
                .pointCreatedTimestamp(point.getPointCreatedTimestamp() != null ? point.getPointCreatedTimestamp().toEpochMilli() : null)
                .pointUpdatedTimestamp(point.getPointUpdatedTimestamp() != null ? point.getPointUpdatedTimestamp().toEpochMilli() : null)
                .memoIdsInOrder(memoIdsInOrder)
                .build();
    }

}
