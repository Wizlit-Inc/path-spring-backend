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
@Table("point") // create if not exists or update if there is any changes made on application starts
public class Point {
    @Id
    @Column("id")
    private Long id; // auto generate string id

    @NonNull
    @Column("title")
    private String title;

    @Column("objective")
    private String objective;

    @Column("document")
    private String document;

    @Column("created_on")
    private Timestamp createdOn;
}
