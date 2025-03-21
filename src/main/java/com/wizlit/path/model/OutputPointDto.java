package com.wizlit.path.model;

import com.wizlit.path.entity.Point;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutputPointDto {
    private String id;
    private String title;
    private String document;

    // function: convert Point to OutputPointDto
    public static OutputPointDto fromPoint(Point point) {
        return OutputPointDto.builder()
                .id(point.getId().toString())
                .title(point.getTitle())
                .document(point.getDescription())
                .build();
    }
}
