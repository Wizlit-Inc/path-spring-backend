package com.wizlit.path.entity;

import lombok.*;

import java.io.Serializable;

@Builder
@Value
public class EdgePK implements Serializable {
    private Long startPoint; // Foreign key for Point.id
    private Long endPoint; // Foreign key for Point.id
}
