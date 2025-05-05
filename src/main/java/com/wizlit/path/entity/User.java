package com.wizlit.path.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("path_user")
public class User {
    @Id
    @Column("user_id")
    private Long userId;

    @Column("user_name")
    private String userName;

    @Column("user_email")
    private String userEmail;

    @Column("user_avatar")
    private String userAvatar;

    @Column("user_created_timestamp")
    private Instant userCreatedTimestamp;

    @Column("user_updated_timestamp")
    private Instant userUpdatedTimestamp; // update on user name, user email, user uploads a new avatar image file
}