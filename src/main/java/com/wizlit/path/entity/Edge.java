package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("edge") // create if not exists or update if there is any changes made on application starts
public class Edge {

    @Id
    @Column("id")
    private Long id;

    @NonNull
    @Column("origin_point")
    private Long originPoint;

    @NonNull
    @Column("destination_point")
    private Long destinationPoint;

    @Column("created_on")
    private Timestamp created_on;

}
