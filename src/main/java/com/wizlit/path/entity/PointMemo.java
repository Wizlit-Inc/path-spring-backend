package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("point_memo")
public class PointMemo {
    @Column("point_id")
    private Long pointId;

    @Column("memo_id")
    private Long memoId;

    @Column("memo_order")
    private Integer memoOrder;
} 