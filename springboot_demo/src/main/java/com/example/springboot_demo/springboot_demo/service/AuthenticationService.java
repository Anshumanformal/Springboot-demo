package com.example.springboot_demo.springboot_demo.service;

import com.example.springboot_demo.springboot_demo.payload.requests.*;
import com.example.springboot_demo.springboot_demo.payload.responses.RegisterResponse;
import org.springframework.http.ResponseEntity;

public interface AuthenticationService {
      ResponseEntity<RegisterResponse> registerUser(RegisterRequest registerRequest);
      ResponseEntity<?> verifyUserRegistration(RegisterVerifyRequest registerVerifyRequest);
      ResponseEntity<?> loginUser(LoginRequest loginRequest);
      ResponseEntity<?> resendOtp(ForgotPasswordRequest forgotPasswordRequest);
      ResponseEntity<?> verifyOtp(RegisterVerifyRequest registerVerifyRequest);
      ResponseEntity<?> resetPassword(ResetPasswordRequest resetPasswordRequest);
      ResponseEntity<?> myProfile(ForgotPasswordRequest forgotPasswordRequest);


}
