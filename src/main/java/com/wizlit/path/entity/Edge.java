package com.wizlit.path.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @Column("start_point")
    private Long startPoint;

    @Column("end_point")
    private Long endPoint;

    @Column("created_on")
    private Timestamp created_on;

}
