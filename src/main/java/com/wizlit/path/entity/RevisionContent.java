package com.wizlit.path.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("revision_content")
public class RevisionContent {
    @Id
    @Column("content_id")
    private Long contentId;

    @Column("content_size")
    private Long contentSize; // actual file size of revision content

    @Column("content_address")
    private String contentAddress; // (contentCompressed is important) content in json before compression. after compression, storage address of revision content

    @Column("content_compressed")
    private Boolean contentCompressed;
}