package com.wizlit.path.temp;

import com.wizlit.path.utils.PrivateAccess;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@RequestMapping("/api/google")
public class GoogleController {

    private final GoogleService googleService;

    @PrivateAccess
    @PostMapping("/user")
    public Mono<ResponseEntity<String>> getUserInfo(
            @RequestAttribute("token") String token
    ) {
        return googleService.getUserInfo(token).map(ResponseEntity::ok);
    }

    @PrivateAccess
    @PostMapping(value = "/drive", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<GoogleDriveFileResponse>> copyFile(
            @RequestAttribute("token") String token,
            @RequestParam String pointId
    ) {
        String sourceId = "16ENglpBm0RpyVEEPLxAJS7K3jmAzBbcn2LnzTTJDlMY";
        String folderId = "1K1BRxA00KcwnDovm5hyTK00QavH-oHvc";

        return googleService.copyDocs(token, sourceId, folderId, pointId).map(ResponseEntity::ok);
    }
}
