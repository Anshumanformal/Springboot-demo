package com.example.springboot_demo.springboot_demo.service;

import com.example.springboot_demo.springboot_demo.exceptions.ResourceNotFoundException;
import com.example.springboot_demo.springboot_demo.entity.Employee;
import com.example.springboot_demo.springboot_demo.payload.responses.GeneralAPIResponse;
import com.example.springboot_demo.springboot_demo.payload.responses.RefreshTokenResponse;
import com.example.springboot_demo.springboot_demo.payload.responses.RegisterVerifyResponse;
import com.example.springboot_demo.springboot_demo.repository.EmployeeRepository;
import com.example.springboot_demo.springboot_demo.security.JwtHelper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtHelper jwtHelper;
    private final UserDetailsService userDetailsService;
    private final EmployeeRepository userRepository;

    public RegisterVerifyResponse generateJwtToken(Employee employee) {
        String myAccessToken = jwtHelper.generateAccessToken(employee);
        String myRefreshToken = jwtHelper.generateRefreshToken(employee);
        return RegisterVerifyResponse.builder()
                .accessToken(myAccessToken)
                .refreshToken(myRefreshToken)
                .firstName(employee.getName().getFirstName())
                .lastName(employee.getName().getLastName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .isVerified(employee.getIsVerified())
                .build();
    }

    public ResponseEntity<?> generateAccessTokenFromRefreshToken(String refreshToken) {
        if (refreshToken != null) {
            try {
                String username = jwtHelper.extractUsername(refreshToken);
                if (username.startsWith("#refresh")) {
                    String finalUserName = username.substring(8);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(finalUserName);
                    Employee employee = userRepository.findByEmail(finalUserName).orElseThrow(
                            () -> new ResourceNotFoundException("User not found with email " + finalUserName));
                    if (jwtHelper.isRefreshTokenValid(refreshToken, userDetails)) {
                        String accessToken = jwtHelper.generateAccessToken(userDetails);
                        return new ResponseEntity<>(RefreshTokenResponse.builder()
                                .accessToken(accessToken)
                                .firstName(employee.getName().getFirstName())
                                .lastName(employee.getName().getLastName())
                                .email(employee.getEmail())
                                .role(employee.getRole())
                                .build(), HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(
                                GeneralAPIResponse.builder().message("Refresh token is expired").build(),
                                HttpStatus.BAD_REQUEST);
                    }
                } else {
                    return new ResponseEntity<>(GeneralAPIResponse.builder().message("Invalid refresh token").build(),
                            HttpStatus.BAD_REQUEST);
                }
            } catch (IllegalArgumentException | MalformedJwtException e) {
                return new ResponseEntity<>(GeneralAPIResponse.builder().message("Invalid refresh token").build(),
                        HttpStatus.BAD_REQUEST);
            } catch (ResourceNotFoundException e) {
                return new ResponseEntity<>(GeneralAPIResponse.builder().message("User not found").build(),
                        HttpStatus.NOT_FOUND);
            } catch (ExpiredJwtException e) {
                return new ResponseEntity<>(GeneralAPIResponse.builder().message("Refresh token is expired").build(),
                        HttpStatus.BAD_REQUEST);
            }

        } else {
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("Refresh token is null").build(),
                    HttpStatus.BAD_REQUEST);
        }

    }

}
