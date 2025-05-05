package com.wizlit.path.model.response;

import com.wizlit.path.model.domain.UserDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long userId;
    private String userName;
    private String userEmail;
    private String userAvatar;

    public static UserResponse fromUser(UserDto userDto) {
        return UserResponse.builder()
                .userId(userDto.getUserId())
                .userName(userDto.getUserName())
                .userEmail(userDto.getUserEmail())
                .userAvatar(userDto.getUserAvatar())
                .build();
    }
}
