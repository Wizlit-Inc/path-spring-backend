package com.wizlit.path.service.manager;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.wizlit.path.entity.Memo;
import com.wizlit.path.entity.MemoDraft;
import com.wizlit.path.entity.MemoReserve;
import com.wizlit.path.entity.MemoRevision;
import com.wizlit.path.entity.RevisionContent;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.model.domain.MemoDto;
import com.wizlit.path.model.domain.ReserveMemoDto;
import com.wizlit.path.model.domain.RevisionContentDto;
import com.wizlit.path.model.domain.MemoRevisionDto;
import com.wizlit.path.repository.MemoDraftRepository;
import com.wizlit.path.repository.MemoRepository;
import com.wizlit.path.repository.MemoReserveRepository;
import com.wizlit.path.repository.MemoRevisionRepository;
import com.wizlit.path.repository.RevisionContentRepository;
import com.wizlit.path.utils.Validator;

import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import reactor.util.function.Tuple2;

@Component
@RequiredArgsConstructor
public class MemoManager {

    /**
     * Helper 규칙:
     * 1. 동일한 repository 가 다른 곳에서도 쓰이면 안됨
     * 2. repository 의 각 기능은 반드시 한 번만 호출
     * 3. repository 기능에는 .onErrorMap(error -> Validator.from(error).toException()) 필수
     * 4. 다른 helper 나 service 호출 금지
     * 5. DTO 반환 금지
     */

    private final MemoRepository memoRepository;
    private final MemoDraftRepository memoDraftRepository;
    private final MemoRevisionRepository memoRevisionRepository;
    private final RevisionContentRepository revisionContentRepository;
    private final MemoReserveRepository memoReserveRepository;

    @Value("${app.memo.reserve.expiry-time:900}") // 15 minutes in seconds
    private int LIMIT_RESERVE_EXPIRY_TIME;

    @Value("${app.memo.draft.stored-time:259200}") // 72 hours in seconds
    private int LIMIT_DRAFT_STORED_TIME;

    /**
     * Lists all memos for a given point ID, filtered by an optional timestamp.
     *
     * @param pointId The ID of the point to filter memos by
     * @param updatedAfter The timestamp after which the memos were updated
     * @return A Flux containing the filtered MemoDto objects
     */
    public Flux<MemoDto> listMemosByPointId(Long pointId, Instant updatedAfter) {
        return memoRepository.findFullMemosByPoint(pointId, updatedAfter)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.POINT_NOT_FOUND, pointId),
                    "foreign", "key", "point"
                )
                .toException());
    }

    /**
     * Retrieves a full memo by its ID.
     *
     * @param memoId The ID of the memo to find
     * @param updatedAfter The timestamp after which the memo was updated
     * @return A Mono containing the found MemoDto, or an error if not found
     */
    public Mono<MemoDto> getFullMemo(Long memoId, Instant updatedAfter) {
        return memoRepository.findFullMemoById(memoId, updatedAfter)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.MEMO_NOT_FOUND, memoId),
                    "foreign", "key", "memo"
                )
                .toException());
    }

    public Mono<MemoDto> getFullMemo(Long memoId) {
        return getFullMemo(memoId, null);
    }

    /**
     * Finds a memo by its ID.
     *
     * @param memoId The ID of the memo to find
     * @return A Mono containing the found Memo, or an error if not found
     */
    public Mono<Memo> findMemoById(Long memoId) {
        return memoRepository.findById(memoId)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.MEMO_NOT_FOUND, memoId),
                    "foreign", "key", "memo"
                )
                .toException())
            .switchIfEmpty(Mono.error(new ApiException(ErrorCode.MEMO_NOT_FOUND, memoId)));
    }

    /**
     * Saves a memo to the database.
     *
     * @param memo The memo to save
     * @return A Mono containing the saved Memo, or an error if the save fails
     */
    private Mono<Memo> _saveMemo(Memo memo) {
        memo.setMemoUpdatedTimestamp(Instant.now());
        if (memo.getMemoTitle() != null) memo.setMemoTitle(memo.getMemoTitle().trim());
        if (memo.getMemoSummary() != null) memo.setMemoSummary(memo.getMemoSummary().trim());

        return memoRepository.save(memo)
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.NULL_INPUT, List.of("memo_point")),
                    "memo_point", "not-null"
                )
                .containsAllElseError(
                    new ApiException(ErrorCode.NULL_INPUT, List.of("memo_title")),
                    "memo_title", "not-null"
                )
                .containsAllElseError(
                    new ApiException(ErrorCode.NULL_INPUT, List.of("memo_type")),
                    "memo_type", "not-null"
                )
                .containsAllElseError(
                    new ApiException(ErrorCode.POINT_NOT_FOUND, memo.getMemoPoint()),
                    "foreign", "key", "point"
                )
                .containsAllElseError(
                    new ApiException(ErrorCode.MEMO_TITLE_DUPLICATE, memo.getMemoTitle()),
                    "duplicate", "key", "unique", "memo_title_key"
                )
                .toException());
    }

    private Mono<Memo> _updateTitle(Memo memo, String title) {
        if (!title.equals(memo.getMemoTitle())) {
            memo.setMemoTitle(title);
            return _saveMemo(memo);
        }
        return Mono.just(memo);
    }

    public Mono<Memo> changeTitle(Long memoId, String title) {
        return findMemoById(memoId)
            .flatMap(memo -> _updateTitle(memo, title));
    }

    public Mono<Memo> changeEmbedContent(Long memoId, String embedContent) {
        return findMemoById(memoId)
            .flatMap(memo -> {
                if (memo.getMemoEmbedContent() == null) {
                    return Mono.error(new ApiException(ErrorCode.NOT_EMBED_MEMO, memoId));
                }

                memo.setMemoEmbedContent(embedContent);
                return _saveMemo(memo);
            });
    }

    public Mono<Memo> moveMemo(Long memoId, Long newPointId) {
        return findMemoById(memoId)
            .flatMap(memo -> {
                memo.setMemoPoint(newPointId);
                return _saveMemo(memo);
            });
    }

    /**
     * Finds a memo reserve by its ID.
     *
     * @param memoId The ID of the memo to find the reserve for
     * @return A Mono containing the found MemoReserve, or an error if not found
     */
    private Mono<MemoReserve> _findReserve(Long memoId) {
        return memoReserveRepository.findById(memoId)
            .onErrorMap(error -> Validator.from(error)
                .toException());
    }

    /**
     * Checks if a user is allowed to edit a memo based on the reserve code.
     *
     * @param memoId The ID of the memo to check
     * @param userId The ID of the user to check
     * @param reserveCode The reserve code to check
     * @return A Mono containing true if the user is allowed to edit the memo, or an error if not
     */
    
    private Mono<MemoReserve> _isAllowedToEdit(Long memoId, Long userId, String reserveCode) {
        return _findReserve(memoId)
            .flatMap(reserve -> {
                if (Instant.now().isBefore(reserve.getReserveExpireTimestamp()) && 
                    (!reserve.getReserveEditor().equals(userId) || 
                     !reserve.getReserveCode().equals(reserveCode))) {
                    return Mono.error(new ApiException(ErrorCode.MEMO_RESERVED, memoId, reserve.getReserveEditor()));
                }
                return Mono.just(reserve);
            })
            .switchIfEmpty(Mono.empty());
    }

    /**
     * Deletes a memo reserve.
     *
     * @param memoId The ID of the memo to delete the reserve for
     * @param userId The ID of the user to delete the reserve for
     * @param currentReserveCode The current reserve code to validate against
     * @return A Mono containing void, or an error if the delete fails
     */
    public Mono<Void> deleteReserve(Long memoId, Long userId, String currentReserveCode) {
        return _isAllowedToEdit(memoId, userId, currentReserveCode)
            .flatMap(reserve -> memoReserveRepository.deleteById(memoId)
                .onErrorMap(error -> Validator.from(error)
                    .toException()))
            .switchIfEmpty(Mono.empty());
    }

    /**
     * Creates a new memo reserve.
     *
     * @param memoId The ID of the memo to reserve
     * @param userId The ID of the user to reserve the memo for
     * @param currentReserveCode Optional current reserve code to validate against
     * @return A Mono containing the saved MemoReserve, or an error if the save fails
     */
    public Mono<ReserveMemoDto> createReserve(Long memoId, Long userId, String currentReserveCode) {
        MemoReserve reserve = new MemoReserve(memoId, userId, LIMIT_RESERVE_EXPIRY_TIME);

        return deleteReserve(memoId, userId, currentReserveCode)
            .then(Mono.defer(() -> {
                return memoReserveRepository.save(reserve.markAsNew())
                    // ← now re-fetch so we get the DB-generated reserveCode
                    .flatMap(saved -> _findReserve(saved.getReserveMemo()))
                    .map(ReserveMemoDto::from)
                    .onErrorMap(error -> Validator.from(error)
                        .containsAllElseError(
                            new ApiException(ErrorCode.MEMO_NOT_FOUND, memoId),
                            "foreign", "key", "reserve_memo"
                        )
                        .toException()
                    );
            }));
    }

    /**
     * Lists revisions for a given memo ID with pagination and timestamp filtering.
     *
     * @param memoId The ID of the memo to list revisions for
     * @param beforeRevTimestamp Only return revisions before this timestamp (exclusive), can be null
     * @param limit Maximum number of revisions to return
     * @return A Flux containing MemoRevisionDto objects for the revisions of the memo
     */
    public Flux<MemoRevisionDto> listRevision(Long memoId, Instant beforeRevTimestamp, int limit) {
        if (limit <= 0) {
            return Flux.empty();
        }

        Flux<MemoRevision> revisions = beforeRevTimestamp != null
            ? memoRevisionRepository.findAllByRevMemoAndTimestampBefore(memoId, beforeRevTimestamp, limit)
            : memoRevisionRepository.findAllByRevMemo(memoId, limit);

        return revisions
            .onErrorMap(error -> Validator.from(error)
                .containsAllElseError(
                    new ApiException(ErrorCode.MEMO_NOT_FOUND, memoId),
                    "foreign", "key", "memo"
                )
                .toException())
            .map(MemoRevisionDto::from);
    }
    
    public Flux<MemoRevisionDto> listRevision(Long memoId) {
        return listRevision(memoId, null, 30);
    }

    /**
     * Retrieves the latest revision for a memo.
     *
     * @param memoId The ID of the memo to get the latest revision for
     * @return A Mono containing a Tuple2 of MemoRevision and String, or an error if the retrieval fails
     */
    public Mono<RevisionContentDto> getRevisionContent(Long revisionContentId) {
        return revisionContentRepository.findById(revisionContentId)
            .onErrorMap(error -> Validator.from(error)
                .toException())
            .flatMap(revisionContent -> {
                if (revisionContent.getContentCompressed()) {
                     // will get from object storage
                    return Mono.just(RevisionContentDto.from(revisionContent, null));
                }
                return Mono.just(RevisionContentDto.from(revisionContent, revisionContent.getContentAddress()));
            });
    }

    private Mono<Tuple2<MemoRevision, RevisionContentDto>> _getLatestRevision(Long memoId) {
        return memoRevisionRepository.findLatestByRevMemo(memoId)
            .onErrorMap(error -> Validator.from(error)
                .toException())
            .flatMap(latestRevision -> {
                if (latestRevision == null) {
                    return Mono.empty();
                }
                return getRevisionContent(latestRevision.getRevContent())
                    .map(revContent -> Tuples.of(latestRevision, revContent));
            });
    }

    /**
     * Applies a new revision to a memo.
     *
     * @param newRevision The new revision to apply
     * @param draft The draft to apply the revision to
     * @return A Mono containing the updated Memo, or an error if the update fails
     */
    private Mono<Memo> _applyNewRevision(MemoRevision newRevision) {
        return findMemoById(newRevision.getRevMemo())
            .flatMap(memo -> {
                memo.setMemoLatestRevision(newRevision.getRevId());
                return _saveMemo(memo);
            });
    }

    /**
     * Adds a new revision to a memo.
     *
     * @param existingDraft The draft to add the revision to
     * @param newUserId The ID of the user to add the revision for
     * @param force If true, forces creation of a new revision regardless of other conditions
     * @return A Mono containing the saved MemoRevision, or an error if the save fails
     */
    private Mono<MemoRevision> _addNewRevision(MemoDraft existingDraft, Long newUserId, boolean force) {
        Long oldUserId = existingDraft.getDraftEditor();
        String trimmedContent = existingDraft.getDraftContent().trim();

        Boolean isSameUser = oldUserId.equals(newUserId);
        Boolean tooOldDraft = Instant.now().isAfter(existingDraft.getDraftCreatedTimestamp().plusSeconds(LIMIT_DRAFT_STORED_TIME));
        Boolean emptyContent = trimmedContent.equals("");
        
        if (!force && isSameUser && !tooOldDraft) {
            return Mono.empty(); // update draft
        }

        if (emptyContent) {
            return Mono.just(new MemoRevision()); // new draft
        }

        return _getLatestRevision(existingDraft.getDraftMemoId())
            .switchIfEmpty(Mono.just(Tuples.of(new MemoRevision(), new RevisionContentDto())))
            .flatMap(tuple -> {
                MemoRevision latestRevision = tuple.getT1();
                RevisionContentDto latestRevisionContent = tuple.getT2();

                Boolean emptyRevision = latestRevision.getRevId() == null;
                Boolean sameContent = existingDraft.getDraftContent().equals(latestRevisionContent.getContent());
                
                // Only create revision if content is different
                if (emptyRevision || !sameContent) {
                    // First create and save revision content
                    RevisionContent revisionContent = RevisionContent.builder()
                        .contentAddress(existingDraft.getDraftContent())
                        .build();
        
                    return revisionContentRepository.save(revisionContent)
                        .onErrorMap(error -> Validator.from(error)
                            .toException())
                        .flatMap(savedContent -> {
                            // Then create and save revision with content ID
                            MemoRevision newRevision = MemoRevision.builder()
                                .revMemo(existingDraft.getDraftMemoId())
                                .revActor(existingDraft.getDraftEditor())
                                .revTimestamp(Instant.now())
                                .revStartTimestamp(existingDraft.getDraftCreatedTimestamp())
                                .revEndTimestamp(existingDraft.getDraftUpdatedTimestamp())
                                .revParentId(latestRevision != null ? latestRevision.getRevId() : null)
                                .revContent(savedContent.getContentId())
                                .build();

                            return memoRevisionRepository.save(newRevision)
                                .onErrorMap(error -> Validator.from(error)
                                    .toException());
                        })
                        .flatMap(savedRevision -> _applyNewRevision(savedRevision)
                            .thenReturn(savedRevision)); // new draft
                }
                return Mono.just(new MemoRevision()); // new draft
            });
    }

    // Add overloaded method for backward compatibility
    private Mono<MemoRevision> _addNewRevision(MemoDraft existingDraft, Long newUserId) {
        return _addNewRevision(existingDraft, newUserId, false);
    }

    public Mono<Void> rollbackToRevision(Long memoId, Long userId, Long revisionId, Boolean forced) {
        // 1. find revision with memoId and revisionId
        
        // if forced, delete current draft & remove all the revision after the revisionId & change memo latest revision
        // else ...
        // 2. save current draft to revision and delete draft
        // 3. create new revision marked as revActor = userId, revStartTimestamp = now, revEndTimestamp = now, revMinorEdit = true, revSummary = "reverted", revContent = revision.getRevContent()
        // 4. change memo latest revision
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Retrieves a draft for a memo.
     *
     * @param memoId The ID of the memo to get the draft for
     * @return A Mono containing the found MemoDraft, or an error if not found
     */
    public Mono<MemoDraft> getDraft(Long memoId) {
        return memoDraftRepository.findById(memoId)
            .onErrorMap(error -> Validator.from(error)
                .toException());
    }

    /**
     * Creates a new draft for a memo.
     *
     * @param memoId The ID of the memo to create the draft for
     * @param userId The ID of the user to create the draft for
     * @param title The title of the draft
     * @param content The content of the draft
     * @return A Mono containing the saved MemoDraft
     */
    private Mono<MemoDraft> _saveDraft(MemoDraft draft) {
        if (draft.getDraftContent() != null && draft.getDraftContent().trim().length() < 8) {
            throw new ApiException(ErrorCode.MIN_LENGTH, "draft_content", 8);
        }

        draft.setDraftUpdatedTimestamp(Instant.now());
        if (draft.getDraftContent() != null) draft.setDraftContent(draft.getDraftContent().trim());

        return memoDraftRepository.findById(draft.getDraftMemoId())
            .flatMap(existing -> 
                memoDraftRepository.save(existing
                    .updateFrom(draft)
                    .markAsExisting()  // now UPDATE
                )
            )
            .switchIfEmpty(memoDraftRepository.save(draft.markAsNew()))
            .onErrorMap(error -> Validator.from(error)
                .toException());
    }

    private Mono<MemoDraft> _saveNewDraft(Long memoId, Long userId, String content) {
        MemoDraft newDraft = MemoDraft.builder()
            .draftMemoId(memoId)
            .draftEditor(userId)
            .draftContent(content)
            .build();
        return _saveDraft(newDraft);
    }

    /**
     * Creates a new embed memo.
     *
     * @param pointId The ID of the point to create the memo for
     * @param userId The ID of the user to create the memo for
     * @param title The title of the memo
     * @param embedContent The embed content of the memo
     * @return A Mono containing the saved Memo
     */
    public Mono<Tuple2<Memo, MemoDraft>> createEmbedMemo(Long pointId, Long userId, String title, String embedContent) {
        Memo newMemo = new Memo(pointId, title, userId, embedContent);
        return _saveMemo(newMemo)
            .flatMap(savedMemo -> {
                // Ensure we have the memo ID before creating the draft
                if (savedMemo.getMemoId() == null) {
                    return Mono.error(new ApiException(ErrorCode.INTERNAL_SERVER, "Failed to get memo ID after save"));
                }

                return _saveNewDraft(savedMemo.getMemoId(), userId, "")
                    .map(savedDraft -> Tuples.of(savedMemo, savedDraft));
            });
    }

    /**
     * Creates a new memo with a draft.
     *
     * @param pointId The ID of the point to create the memo for
     * @param userId The ID of the user to create the memo for
     * @param title The title of the memo
     * @param content The content of the memo
     * @return A Mono containing a Tuple2 of Memo and MemoDraft
     */
    public Mono<Tuple2<Memo, MemoDraft>> createMemoWithDraft(Long pointId, Long userId, String title, String content) {
        Memo newMemo = new Memo(pointId, title, userId);

        return _saveMemo(newMemo)
            .flatMap(savedMemo -> {
                // Ensure we have the memo ID before creating the draft
                if (savedMemo.getMemoId() == null) {
                    return Mono.error(new ApiException(ErrorCode.INTERNAL_SERVER, "Failed to get memo ID after save"));
                }

                return _saveNewDraft(savedMemo.getMemoId(), userId, content)
                    .map(savedDraft -> Tuples.of(savedMemo, savedDraft));
            });
    }

    /**
     * Deletes a draft for a memo.
     *
     * @param memoId The ID of the memo to delete the draft for
     * @return A Mono containing void, or an error if the delete fails
     */
    private Mono<Void> _deleteDraft(Long memoId) {
        return memoDraftRepository.deleteById(memoId)
            .onErrorMap(error -> Validator.from(error)
                .toException());
    }
    
    /**
     * Updates a draft for a memo.
     *
     * @param memoId The ID of the memo to update the draft for
     * @param userId The ID of the user to update the draft for
     * @param title The title of the draft
     * @param content The content of the draft
     * @param currentReserveCode The current reserve code to validate against
     * @return A Mono containing the updated MemoDraft, or an error if the update fails
     */
    public Mono<MemoDraft> updateDraft(Long memoId, Long userId, String content, String currentReserveCode) {
        // Prevent abnormal update where too much content was deleted
        return deleteReserve(memoId, userId, currentReserveCode)
            .then(getDraft(memoId))
            .flatMap(existingDraft -> {
                // Calculate content length difference
                int oldLength = existingDraft.getDraftContent() != null ? existingDraft.getDraftContent().length() : 0;
                int newLength = content != null ? content.length() : 0;
                int lengthDiff = oldLength - newLength;

                // If more than 80% of content was deleted and original content was long (>2000 chars), force new revision
                boolean isAbnormalUpdate = oldLength > 2000 && lengthDiff > 0 && (double) lengthDiff / oldLength > 0.8;
                
                return _addNewRevision(existingDraft, userId, isAbnormalUpdate)
                    .flatMap(savedRevision -> _deleteDraft(memoId)
                        .then(_saveNewDraft(memoId, userId, content)))
                    .switchIfEmpty(Mono.defer(() -> {
                        existingDraft.setDraftContent(content);
                        return _saveDraft(existingDraft);
                    }));
            })
            .switchIfEmpty(_saveNewDraft(memoId, userId, content));
    }

}