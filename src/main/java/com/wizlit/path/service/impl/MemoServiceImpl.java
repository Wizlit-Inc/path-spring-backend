package com.wizlit.path.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.wizlit.path.model.domain.MemoDto;
import com.wizlit.path.model.domain.ReserveMemoDto;
import com.wizlit.path.model.domain.UserDto;
import com.wizlit.path.service.MemoService;
import com.wizlit.path.service.manager.MemoManager;
import com.wizlit.path.entity.Memo;
import com.wizlit.path.entity.MemoDraft;
import com.wizlit.path.entity.MemoReserve;
import com.wizlit.path.service.manager.PointManager;
import com.wizlit.path.temp.GoogleService;
import com.wizlit.path.exception.ApiException;
import com.wizlit.path.exception.ErrorCode;
import com.wizlit.path.model.domain.MemoRevisionDto;
import com.wizlit.path.model.domain.RevisionContentDto;

@Service
@RequiredArgsConstructor
public class MemoServiceImpl implements MemoService {

    /**
     * Service 규칙:
     * 1. repository 직접 호출 X (helper 를 통해서만 호출 - 동일한 helper 가 다른 곳에서도 쓰여도 됨)
     */
    
    private final MemoManager memoManager;
    private final PointManager pointManager;
    // private final GoogleService driveService;

    @Override
    public Flux<MemoDto> listMemosByPointId(Long pointId, Instant updatedAfter) {
        return memoManager.listMemosByPointId(pointId, updatedAfter);
    }

    @Override
    public Mono<MemoDto> getMemo(Long memoId, Instant updatedAfter) {
        return memoManager.getFullMemo(memoId, updatedAfter);
    }

    @Transactional
    @Override
    public Mono<MemoDto> createEmbedMemo(Long pointId, UserDto user, String title, String embedContent) {
        return memoManager.createEmbedMemo(pointId, user.getUserId(), title, embedContent)
            .flatMap(tuple -> {
                Memo savedMemo = tuple.getT1();
                MemoDraft savedDraft = tuple.getT2();

                return pointManager.addMemoToPoint(pointId, savedMemo.getMemoId())
                    .then(Mono.defer(() -> {
                        MemoDto dto = MemoDto.from(
                            savedMemo,
                            savedDraft,
                            user.getUserId()
                        );

                        return Mono.just(dto);
                    }));
            });
    }

    @Transactional
    @Override
    public Mono<MemoDto> createMemo(Long pointId, UserDto user, String title, String content) {        
        return memoManager.createMemoWithDraft(pointId, user.getUserId(), title, content)
            .flatMap(tuple -> {
                Memo savedMemo = tuple.getT1();
                MemoDraft savedDraft = tuple.getT2();

                return pointManager.addMemoToPoint(pointId, savedMemo.getMemoId())
                    .then(Mono.defer(() -> {
                        MemoDto dto = MemoDto.from(
                            savedMemo,
                            savedDraft,
                            user.getUserId()
                        );

                        return Mono.just(dto);
                    }));
            });
    }

    @Transactional
    @Override
    public Mono<MemoDto> updateMemo( Long memoId, UserDto user, String content, String reserveCode) {
        return memoManager.updateDraft(memoId, user.getUserId(), content, reserveCode)
            .flatMap(draftDto -> memoManager.getFullMemo(memoId));
    }

    @Transactional
    @Override
    public Mono<MemoDto> updateTitle(Long memoId, String title) {
        return memoManager.changeTitle(memoId, title)
            .flatMap(memo -> memoManager.getFullMemo(memoId));
    }

    @Transactional
    @Override
    public Mono<MemoDto> updateEmbedContent(
        Long memoId,
        String embedContent
    ) {
        return memoManager.changeEmbedContent(memoId, embedContent)
            .flatMap(memo -> memoManager.getFullMemo(memoId));
        // return memoManager.findMemoById(memoId)
        //     .<Memo>flatMap(memo -> {
        //         if (memo.getMemoEmbedContent() != null) {
        //             String documentUrl = memo.getMemoEmbedContent();

        //             // If title is being updated and document exists, update Google Drive file name
        //             if (title != null && documentUrl != null) {
        //                 // Extract file ID from Google Docs URL
        //                 String fileId = documentUrl.substring(documentUrl.lastIndexOf("/") + 1);
        //                 return driveService.updateFileName(externalKey, fileId, memoId + " // " + title)
        //                     .then(memoManager.updateExternalMemo(memo, title));
        //             }
        //         }
        //         throw new ApiException(ErrorCode.NOT_EXTERNAL_MEMO, memoId);
        //     })
    }

    // private Mono<Point> processDocument(String token, Point savedPoint) {
    //     if (savedPoint.getDocument() == null) {
    //         return driveService.copyDocs(
    //                         token,
    //                         "16ENglpBm0RpyVEEPLxAJS7K3jmAzBbcn2LnzTTJDlMY",
    //                         googleDriveFolderId,
    //                         savedPoint.getId() + " // " + savedPoint.getTitle()
    //                 )
    //                 .flatMap(driveResponse -> {
    //                     savedPoint.setDocument("https://docs.google.com/document/d/" + driveResponse.getId());
    //                     return pointService.updatePoint(savedPoint);
    //                 });
    //     }
    //     return Mono.just(savedPoint);
    // }

    @Transactional
    @Override
    public Mono<ReserveMemoDto> reserveMemo(Long memoId, UserDto user, String currentReserveCode) {
        return memoManager.createReserve(memoId, user.getUserId(), currentReserveCode);
    }

    @Transactional
    @Override
    public Mono<Void> cancelReserve(Long memoId, UserDto user, String reserveCode) {
        return memoManager.deleteReserve(memoId, user.getUserId(), reserveCode);
    }

    @Transactional
    @Override
    public Mono<Void> moveMemo(Long memoId, Long newPointId) {
        return Mono.zip(
            pointManager.moveMemoToPoint(newPointId, memoId),
            memoManager.moveMemo(memoId, newPointId)
        ).then();
    }

    @Override
    public Flux<MemoRevisionDto> listRevisions(Long memoId, Instant beforeTimestamp, int limit) {
        return memoManager.listRevision(memoId, beforeTimestamp, limit);
    }

    @Override
    public Mono<RevisionContentDto> getRevisionContent(Long revisionContentId) {
        return memoManager.getRevisionContent(revisionContentId);
    }

    @Override
    public Mono<Void> rollbackToRevision(Long memoId, UserDto user, Long revisionId, Boolean forced) {
        return memoManager.rollbackToRevision(memoId, user.getUserId(), revisionId, forced);
    }
}
