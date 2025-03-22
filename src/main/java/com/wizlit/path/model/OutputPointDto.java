package com.wizlit.path.model;

import com.wizlit.path.entity.Point;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutputPointDto {
    private String id;
    private String title;
    private String objective;
    private String document;
    private Timestamp created_on;

    // function: convert Point to OutputPointDto
    public static OutputPointDto fromPoint(Point point) {
        return OutputPointDto.builder()
                .id(point.getId().toString())
                .title(point.getTitle())
                .objective(point.getObjective())
                .document(point.getDocument())
                .created_on(point.getCreatedOn())
                .build();
    }
}
