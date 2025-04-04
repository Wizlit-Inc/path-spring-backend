package com.wizlit.path.temp;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
public class GoogleServiceTest {

    private final WebClient.Builder oauthBuilder = WebClient.builder().baseUrl("https://www.googleapis.com/oauth2/v3");
    private final WebClient.Builder driveBuilder = WebClient.builder().baseUrl("https://www.googleapis.com/drive/v3");

    private final String accessToken = "ya29.a0AZYkNZirxmBqCLXvtL8EpKU-vwckC6Bud2tDR_7bgK0l5D0TLJQUYmxmzMKz3Y3tVXlaQpO6DDVh_95jUCkjLLHJznmyylLn9HrXit0XNsvKi_kblrc2wygHBmtyKtuDZVPQWbnrtckFekn6lx31m4tLGxZL4t8A0KoDQI497waCgYKAXgSARASFQHGX2MiiqXL0QiUYUjdJeZFYXp_Zg0177";

    @Test
    @Disabled("Integration test - enable manually with a valid token")
    public void testGetUserInfoIntegration() {
        // Instantiate the service with real WebClients
        GoogleService googleService = new GoogleService(oauthBuilder, driveBuilder);

        // Call the getUserInfo method
        Mono<String> userInfoMono = googleService.getUserInfo(accessToken);

        // Option 1: Block to print out the user info (useful for manual testing)
        String userInfo = userInfoMono.block();
        System.out.println("User Info: " + userInfo);

        // Option 2: Verify the result contains expected data using StepVerifier
        StepVerifier.create(userInfoMono)
                .expectNextMatches(response -> response.contains("email"))
                .verifyComplete();
    }

    @Test
    @Disabled("Integration test - enable manually with a valid token and valid file IDs")
    public void testCopyDocsIntegration() {
        String sourceDocId = "16ENglpBm0RpyVEEPLxAJS7K3jmAzBbcn2LnzTTJDlMY"; // A valid source document ID from your Google Drive
        String folderId = "1RjFWGl2-y0Ej_b_tS_HAsTrYogDbabE9"; // A valid folder ID where you want the copy to reside
        String title = "Test Copy Document Integration";

        GoogleService googleService = new GoogleService(oauthBuilder, driveBuilder);

        Mono<GoogleDriveFileResponse> copyResponseMono = googleService.copyDocs(accessToken, sourceDocId, folderId, title);

        // Optionally block to print the response
        GoogleDriveFileResponse copyResponse = copyResponseMono.block();
        System.out.println("Copy Docs Response: " + copyResponse);

        // Verify that the returned file has the expected title
        StepVerifier.create(copyResponseMono)
                .expectNextMatches(response -> title.equals(response.getName()))
                .verifyComplete();
    }

    @Test
    @Disabled("Integration test - enable manually with a valid token and valid file ID")
    public void testUpdateFileNameIntegration() {
        String fileId = "1m9QBM7KYiKss_fKEs67WmnY_cq4YyR11exhG2ObxR8A"; // A valid file ID that you want to update
        String newName = "Updated File Name Integration Test 1";

        GoogleService googleService = new GoogleService(oauthBuilder, driveBuilder);

        Mono<GoogleDriveFileResponse> updateResponseMono = googleService.updateFileName(accessToken, fileId, newName);

        // Optionally block to print the response
        GoogleDriveFileResponse updateResponse = updateResponseMono.block();
        System.out.println("Update File Name Response: " + updateResponse);

        // Verify that the file's name has been updated
        StepVerifier.create(updateResponseMono)
                .expectNextMatches(response -> newName.equals(response.getName()))
                .verifyComplete();
    }
}