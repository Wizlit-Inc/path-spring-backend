package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("file_memo")
public class FileMemo {
    @Column("file_id")
    private Long fileId;

    @Column("memo_id")
    private Long memoId; // when memo draft content is turned into memo revision content, it will look for the file id and will be append here.

    // Composite primary key
    public static class FileMemoId {
        private Long fileId;
        private Long memoId;
    }
} 