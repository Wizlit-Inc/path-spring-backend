package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("last_update") // create if not exists or update if there is any changes made on application starts
public class LastUpdate {

    @Id
    @Column("id")
    private String id;

    @Column("updated_time")
    private Instant updated_time;

}
