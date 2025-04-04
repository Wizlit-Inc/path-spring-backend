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
public class UpdatePointDto {
    private String title;
    private String objective;

    public Point toPoint(String id) {
        return Point.builder()
                .id(Long.parseLong(id))
                .title(title)
                .objective(objective)
                .build();
    }
} 