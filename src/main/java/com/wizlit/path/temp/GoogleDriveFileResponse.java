package com.wizlit.path.temp;

import lombok.Data;

@Data
public class GoogleDriveFileResponse {
    private String id;
    private String kind;
    private String name;
    private String mimeType;
    private String teamDriveId;
    private String driveId;
}
