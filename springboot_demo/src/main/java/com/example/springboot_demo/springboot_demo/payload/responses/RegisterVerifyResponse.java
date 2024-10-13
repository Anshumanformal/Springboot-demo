package com.example.springboot_demo.springboot_demo.payload.responses;

import com.example.springboot_demo.springboot_demo.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVerifyResponse {
    private String accessToken ;
    private String refreshToken ;
    private String firstName ;
    private String lastName ;
    private String email ;
    private Role role;
    private boolean isVerified;
}
