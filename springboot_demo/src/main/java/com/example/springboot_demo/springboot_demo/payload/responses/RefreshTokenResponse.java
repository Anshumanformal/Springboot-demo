package com.example.springboot_demo.springboot_demo.payload.responses;

import com.example.springboot_demo.springboot_demo.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenResponse {
    private String accessToken;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
}