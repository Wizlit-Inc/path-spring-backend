package com.wizlit.path.repository;


import java.time.Instant;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wizlit.path.entity.Memo;
import com.wizlit.path.model.domain.MemoDto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MemoRepository extends R2dbcRepository<Memo, Long> {
    // 필드명을 snake_case로 유지하고, timestamp 컬럼은 epoch milliseconds(Bigint)로 변환
    String COMMON_SELECT_FIELDS =
        "m.memo_point AS memo_point, " +
        "m.memo_id AS memo_id, " +
        "m.memo_title AS memo_title, " +
        "m.memo_type AS memo_type, " +
        "m.memo_summary AS memo_summary, " +
        "m.memo_embed_content AS memo_embed_content, " +
        "(EXTRACT(EPOCH FROM m.memo_summary_timestamp) * 1000)::bigint AS memo_summary_timestamp, " +
        "(EXTRACT(EPOCH FROM m.memo_created_timestamp) * 1000)::bigint AS memo_created_timestamp, " +
        "(EXTRACT(EPOCH FROM GREATEST(m.memo_updated_timestamp, COALESCE(d.draft_updated_timestamp, m.memo_updated_timestamp))) * 1000)::bigint AS memo_updated_timestamp, " +
        "ARRAY(SELECT mc.user_id FROM memo_contributor mc WHERE mc.memo_id = m.memo_id) AS contributor_user_ids, " +
        "CASE " +
        "  WHEN d.draft_updated_timestamp IS NOT NULL AND d.draft_updated_timestamp > COALESCE(r.rev_timestamp, '1970-01-01'::timestamp) " +
        "  THEN d.draft_editor " +
        "  ELSE r.rev_actor " +
        "END AS memo_updated_user, " +
        "CASE " +
        "  WHEN d.draft_updated_timestamp IS NOT NULL AND d.draft_updated_timestamp > COALESCE(r.rev_timestamp, '1970-01-01'::timestamp) " +
        "  THEN d.draft_content " +
        "  ELSE rc.content_address " +
        "END AS content";

    String COMMON_JOINS =
        "FROM memo m " +
        "LEFT JOIN memo_draft d ON m.memo_id = d.draft_memo_id " +
        "LEFT JOIN memo_revision r ON m.memo_id = r.rev_memo " +
        "LEFT JOIN revision_content rc ON r.rev_content = rc.content_id";

    String COMMON_WHERE_CLAUSE =
        "AND (:updatedAfter IS NULL OR GREATEST(m.memo_updated_timestamp, COALESCE(d.draft_updated_timestamp, m.memo_updated_timestamp)) > :updatedAfter) " +
        "AND (r.rev_id IS NULL OR r.rev_id = (SELECT MAX(rev_id) FROM memo_revision WHERE rev_memo = m.memo_id))";

    @Query("SELECT " + COMMON_SELECT_FIELDS + " " +
           COMMON_JOINS + " " +
           "WHERE m.memo_id = :memoId " +
           COMMON_WHERE_CLAUSE)
    Mono<MemoDto> findFullMemoById(Long memoId, Instant updatedAfter);

    @Query("SELECT " + COMMON_SELECT_FIELDS + " " +
           COMMON_JOINS + " " +
           "WHERE m.memo_point = :memoPoint " +
           COMMON_WHERE_CLAUSE)
    Flux<MemoDto> findFullMemosByPoint(Long memoPoint, Instant updatedAfter);

    // 기존 메서드: 필요에 따라 사용
    Flux<Memo> findByMemoPointAndMemoUpdatedTimestampAfter(Long memoPoint, Instant updatedAfter);
}
