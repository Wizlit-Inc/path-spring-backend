package com.wizlit.path.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wizlit.path.model.ResponseWithChange;
import com.wizlit.path.model.domain.MemoRevisionDto;
import com.wizlit.path.model.response.FinalResponse;
import com.wizlit.path.service.MemoService;
import com.wizlit.path.service.UserService;
import com.wizlit.path.utils.PrivateAccess;
import com.wizlit.path.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memo-revision")
@Tag(name = "MemoRevision", description = "Memo revision management APIs")
public class MemoRevisionController {

    /**
     * Controller 규칙:
     * 1. service 만 호출
     */
    
    private final UserService userService;
    private final MemoService memoService;

    @Operation(
        summary = "List memo revisions",
        description = "Lists all revisions of a memo with pagination and timestamp filtering"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved memo revisions"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Memo not found (ErrorCode: MEMO_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> listRevisions(
        @RequestParam Long memoId,
        @RequestParam(required = false) Long timestampAfter
    ) {
        Instant beforeTimestamp = timestampAfter != null ? Instant.ofEpochMilli(timestampAfter) : null;
        return memoService.listRevisions(memoId, beforeTimestamp, 30)
            .collectList()
            .flatMap(revisions -> {
                List<Long> userIds = revisions.stream()
                    .map(MemoRevisionDto::getRevisionActor)
                    .distinct()
                    .toList();
                return userService.listUserByUserIds(userIds, null)
                    .collectList()
                    .map(users -> new FinalResponse().forMemoRevisions(memoId, revisions, users));
            })
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }

    @Operation(
        summary = "Get revision content",
        description = "Retrieves the content of a specific revision"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved revision content"),
        @ApiResponse(responseCode = "400", description = "Invalid input parameters (ErrorCode: NULL_INPUT)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized access (ErrorCode: INVALID_TOKEN, EXPIRED_TOKEN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Revision not found (ErrorCode: REVISION_NOT_FOUND)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error (ErrorCode: INTERNAL_SERVER, UNKNOWN)",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/content/{revisionContentId}")
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> getRevisionContent(
        @PathVariable Long revisionContentId
    ) {
        return memoService.getRevisionContent(revisionContentId)
            .map(content -> new FinalResponse().forMemoRevisionContent(revisionContentId, content))
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }

    @Operation(
        summary = "Rollback to revision",
        description = "Rollback to a specific revision"
    )
    @PostMapping("/memo/{memoId}/revision/{revisionId}/rollback")
    @PrivateAccess(role = "admin")
    public Mono<ResponseEntity<ResponseWithChange<FinalResponse>>> rollbackToRevision(
        @PathVariable Long memoId,
        @PathVariable Long revisionId,
        @RequestAttribute("email") String email,
        @RequestAttribute("name") String name,
        @RequestAttribute("avatar") String avatar,
        @RequestParam(required = false) Long lastFetchTimestamp,
        @RequestParam(required = false) Boolean forced
    ) {
        Instant updatedAfter = lastFetchTimestamp != null ? Instant.ofEpochMilli(lastFetchTimestamp) : null;
        return userService.getUserByEmailAndCreateIfNotExists(email, name, avatar)
            .flatMap(user -> memoService.rollbackToRevision(memoId, user, revisionId, forced))
            .then(memoService.getMemo(memoId, updatedAfter)
                .flatMap(memo -> userService.listUserByUserIds(memo.getContributorUserIds(), updatedAfter)
                    .collectList()
                    .map(users -> new FinalResponse().forGetMemo(memoId, memo, users, null)))
            )
            .switchIfEmpty(Mono.just(new FinalResponse()))
            .map(ResponseWithChange::new)
            .map(responseWithChange -> responseWithChange.toResponseEntity(HttpStatus.OK));
    }
}
