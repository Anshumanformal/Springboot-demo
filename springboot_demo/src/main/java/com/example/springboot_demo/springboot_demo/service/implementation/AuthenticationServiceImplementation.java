package com.example.springboot_demo.springboot_demo.service.implementation;

import com.example.springboot_demo.springboot_demo.constants.ApplicationConstants;
import com.example.springboot_demo.springboot_demo.exceptions.ResourceNotFoundException;
import com.example.springboot_demo.springboot_demo.entity.Employee;
import com.example.springboot_demo.springboot_demo.entity.Username;
import com.example.springboot_demo.springboot_demo.payload.requests.*;
import com.example.springboot_demo.springboot_demo.payload.responses.GeneralAPIResponse;
import com.example.springboot_demo.springboot_demo.payload.responses.RegisterResponse;
import com.example.springboot_demo.springboot_demo.payload.responses.RegisterVerifyResponse;
import com.example.springboot_demo.springboot_demo.payload.responses.UserProfile;
import com.example.springboot_demo.springboot_demo.repository.EmployeeRepository;
import com.example.springboot_demo.springboot_demo.service.AuthenticationService;
import com.example.springboot_demo.springboot_demo.service.EmailService;
import com.example.springboot_demo.springboot_demo.service.JwtService;
import com.example.springboot_demo.springboot_demo.service.OtpService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImplementation implements AuthenticationService {
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final CacheManager cacheManager;
    private final AuthenticationManager authenticationManager;

    @Override
    public ResponseEntity<RegisterResponse> registerUser(RegisterRequest registerRequest) {
        try {
            log.info("Received request to register employee with email {}", registerRequest.getEmail());
            Optional<Employee> existingUserOpt = employeeRepository.findByEmail(registerRequest.getEmail().trim().toLowerCase());
            if (existingUserOpt.isPresent()) {
                Employee existingUser = existingUserOpt.get();
                log.info("Employee already exists with email {}", registerRequest.getEmail());
                if (existingUser.getIsVerified()) {
                    return new ResponseEntity<>(RegisterResponse.builder()
                            .message("Employee already exists")
                            .build(), HttpStatus.BAD_REQUEST);
                } else {
                    log.info("Employee already exists but not verified with email {}, so their details will be updated", registerRequest.getEmail());
                    updateUserDetails(existingUser, registerRequest);
                    String otpToBeMailed = otpService.getOtpForEmail(registerRequest.getEmail());
                    CompletableFuture<Integer> emailResponse = emailService.sendEmailWithRetry(registerRequest.getEmail(), otpToBeMailed);
                    if (emailResponse.get() == -1) {
                        return new ResponseEntity<>(RegisterResponse.builder()
                                .message("Failed to send OTP email. Please try again later.")
                                .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                    employeeRepository.save(existingUser);
                    return new ResponseEntity<>(RegisterResponse.builder()
                            .message("An email with OTP has been sent to your email address. Kindly verify.")
                            .build(), HttpStatus.CREATED);
                }
            } else {
                log.info("Employee does not exist with email {}, so this employee will be created", registerRequest.getEmail());
                Employee newUser = createUser(registerRequest);
                String otpToBeMailed = otpService.getOtpForEmail(registerRequest.getEmail());
                CompletableFuture<Integer> emailResponse = emailService.sendEmailWithRetry(registerRequest.getEmail(),otpToBeMailed);
                if (emailResponse.get() == -1) {
                    return new ResponseEntity<>(RegisterResponse.builder()
                            .message("Failed to send OTP email. Please try again later.")
                            .build(), HttpStatus.INTERNAL_SERVER_ERROR);
                }
                employeeRepository.save(newUser);
                log.info("Employee saved with the email {}", registerRequest.getEmail());
                return new ResponseEntity<>(RegisterResponse.builder()
                        .message("An email with OTP has been sent to your email address. Kindly verify.")
                        .build(), HttpStatus.CREATED);
            }
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send OTP email for employee with email {}", registerRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RegisterResponse.builder()
                    .message("Failed to send OTP email. Please try again later.")
                    .build());
        }catch(DataIntegrityViolationException ex) {
            log.info("Employee already exists with phone number {}", registerRequest.getPhoneNumber());
            return new ResponseEntity<>(RegisterResponse.builder()
                    .message("Employee already exists with this phone number. Please try again with a different phone number.")
                    .build(), HttpStatus.BAD_REQUEST);
        }
        catch (Exception e) {
            log.error("Failed to register employee with email {}", registerRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RegisterResponse.builder()
                    .message("Failed to register employee. Please try again later.")
                    .build());
        }
    }

    private void updateUserDetails(Employee employee, RegisterRequest registerRequest) {
        DUPLICATE_CODE(registerRequest, employee);
    }

    private Employee createUser(RegisterRequest registerRequest) {
        Employee employee = new Employee();
        DUPLICATE_CODE(registerRequest, employee);
        return employee;
    }

    private void DUPLICATE_CODE(RegisterRequest registerRequest, Employee employee) {
        if (registerRequest.getGender().name().equals("FEMALE")) {
            employee.setProfilePicture(ApplicationConstants.femaleProfilePicture);
        } else {
            employee.setProfilePicture(ApplicationConstants.maleProfilePicture);
        }
        employee.setName(new Username(registerRequest.getFirstName().trim(), registerRequest.getLastName().trim()));
        employee.setEmail(registerRequest.getEmail().trim().toLowerCase());
        employee.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        employee.setRole(registerRequest.getRole());
        employee.setGender(registerRequest.getGender());
        employee.setPhoneNumber(registerRequest.getPhoneNumber());
        employee.setIsVerified(false);
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ResponseEntity<?> verifyUserRegistration(RegisterVerifyRequest registerVerifyRequest) {
        String emailEntered = registerVerifyRequest.getEmail().trim().toLowerCase();
        String otpEntered = registerVerifyRequest.getOtp().trim();
        try {
            Employee employee = employeeRepository.findByEmail(emailEntered).orElseThrow(
                    ResourceNotFoundException::new
            );
            Cache cache = cacheManager.getCache("employee");
            if(cache != null) {
                String cachedOtp = cache.get(emailEntered, String.class);
                if (cachedOtp == null) {
                    log.info("the otp is not present in cache memory, it has expired for employee {}, kindly retry and Register", emailEntered);
                    return new ResponseEntity<>(GeneralAPIResponse.builder().message("Otp has been expired for employee " + emailEntered).build(), HttpStatus.REQUEST_TIMEOUT);
                } else if (!otpEntered.equals(cachedOtp)) {
                    log.info("the entered otp does not match the otp Stored in cache for email {}", emailEntered);
                    return new ResponseEntity<>(GeneralAPIResponse.builder().message("Incorrect otp has been entered").build(), HttpStatus.BAD_REQUEST);
                } else {
                    // OTP provided is same as the one stored in cache memory
                    employee.setIsVerified(true);
                    employeeRepository.save(employee);
                    log.info("the employee email {} is successfully verified", employee.isEnabled());
                    RegisterVerifyResponse jwtToken = jwtService.generateJwtToken(employee);
                    return new ResponseEntity<>(jwtToken, HttpStatus.CREATED);
                }
            }
        } catch (ResourceNotFoundException ex) {
            log.info("employee with email {} not found in database", emailEntered);
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("employee with this email does not exist").build(), HttpStatus.NOT_FOUND);
        }
        return null;
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ResponseEntity<?> loginUser(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().trim().toLowerCase();
        String password = loginRequest.getPassword();
        try {
            Employee employee = employeeRepository.findByEmail(email).orElseThrow(
                        ResourceNotFoundException::new
                );
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
            if (!employee.getIsVerified()) {
                return new ResponseEntity<>(GeneralAPIResponse.builder().message("Employee is not verified").build(), HttpStatus.BAD_REQUEST);
            }

            RegisterVerifyResponse jwtToken = jwtService.generateJwtToken(employee);
            return new ResponseEntity<>(jwtToken, HttpStatus.OK);

        } catch (ResourceNotFoundException ex) {
            log.info("employee whose email is {} not found in Database", email);
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("Employee with this email does not exist").build(), HttpStatus.NOT_FOUND);
        }
        catch (Exception e) {
            log.error("Failed to authenticate employee with email {}", email, e);
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("Invalid credentials").build(), HttpStatus.BAD_REQUEST);
        }


    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ResponseEntity<?> resendOtp(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail().trim().toLowerCase();
        try {
            // Employee employee = 
            employeeRepository.findByEmail(email).orElseThrow(
                    ResourceNotFoundException::new
            );

            Cache cache = cacheManager.getCache("employee");
            if(cache != null) {
                // Get the cached OTP, handling the case where the cache entry might be null
                String cachedOtp = cache.get(email, String.class);
                if (cachedOtp != null) {
                    // OTP is already present, so log the info and return an appropriate response
                    log.info("The OTP is already present in cache memory for employee {}, kindly retry after some time", email);
                    return new ResponseEntity<>(GeneralAPIResponse.builder().message("Kindly retry after 1 minute").build(), HttpStatus.TOO_MANY_REQUESTS);
                } else {
                    // Handle case where OTP for this email is not in the cache (OTP can be generated)
                    log.info("No OTP found in the cache for employee {}", email);
                }
            }
            else log.info("Employee not found in cache");
            
            String otpToBeSend = otpService.getOtpForEmail(email);
            CompletableFuture<Integer> emailResponse= emailService.sendEmailWithRetry(email,otpToBeSend);
            if (emailResponse.get() == -1) {
                return new ResponseEntity<>(GeneralAPIResponse.builder().message("Failed to send OTP email. Please try again later.").build(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return  new ResponseEntity<>(GeneralAPIResponse.builder().message("An email with OTP has been sent to your email address. Kindly verify.").build(), HttpStatus.OK);

        } catch ( UnsupportedEncodingException e) {
            log.error("Failed to send OTP email for employee with email {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RegisterResponse.builder()
                    .message("Failed to send OTP email. Please try again later.")
                    .build());
        } catch (ResourceNotFoundException ex) {
            log.info("employee with email {} not found in Database", email);
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("Employee with email not found in database").build(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to resend OTP for employee with email {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RegisterResponse.builder()
                    .message("Failed to resend OTP. Please try again later.")
                    .build());
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ResponseEntity<?> verifyOtp(RegisterVerifyRequest registerVerifyRequest) {
        String email = registerVerifyRequest.getEmail().trim().toLowerCase();
        String otp = registerVerifyRequest.getOtp().trim();
        try {
            // Employee employee = 
            employeeRepository.findByEmail(email).orElseThrow(
                    ResourceNotFoundException::new
            );
        } catch (ResourceNotFoundException ex) {
            log.info("employee with email {} not found in database ", email);
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("iUser with this email does not exist").build(), HttpStatus.NOT_FOUND);
        }
        Cache cache = cacheManager.getCache("employee");
        if(cache != null){
            // Get the cached OTP, handling the case where the cache entry might be null
            String cachedOtp = cache.get(email, String.class);
            if (cachedOtp == null) {
                // Handle case where the OTP has expired or is not present
                log.info("The OTP is not present in cache memory or has expired for employee {}, kindly retry", email);
                return new ResponseEntity<>(GeneralAPIResponse.builder()
                        .message("OTP has expired for employee " + email)
                        .build(), HttpStatus.REQUEST_TIMEOUT);
            } else if (!otp.equals(cachedOtp)) {
                // Handle case where the entered OTP does not match the cached OTP
                log.info("The entered OTP does not match the OTP stored in cache for email {}", email);
                return new ResponseEntity<>(GeneralAPIResponse.builder()
                        .message("Incorrect OTP has been entered")
                        .build(), HttpStatus.BAD_REQUEST);
            } else {
                // Handle case where the OTP is correct and verified
                log.info("OTP verified successfully for employee {}, they can now change the password", email);
                return new ResponseEntity<>(GeneralAPIResponse.builder()
                        .message("OTP verified successfully, now you can change the password")
                        .build(), HttpStatus.OK);
            }
        } else {
            // Handle case where the "employee" cache does not exist
            log.warn("Cache 'employee' does not exist, potential configuration issue");
            return new ResponseEntity<>(GeneralAPIResponse.builder()
                    .message("OTP cache not found for employee " + email)
                    .build(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // String cachedOtp = cacheManager.getCache("employee").get(email, String.class);
        // if (cachedOtp == null) {
        //     log.info("the otp is not present in cache memory, it has expired for employee {}, kindly retry", email);
        //     return new ResponseEntity<>(GeneralAPIResponse.builder().message("Otp has been expired for employee " + email).build(), HttpStatus.REQUEST_TIMEOUT);
        // } else if (!otp.equals(cachedOtp)) {
        //     log.info("entered otp does not match the otp Stored in cache for email {}", email);
        //     return new ResponseEntity<>(GeneralAPIResponse.builder().message("Incorrect otp has been entered").build(), HttpStatus.BAD_REQUEST);
        // } else {
        //     return new ResponseEntity<>(GeneralAPIResponse.builder().message("otp verified successfully, now you can change the password").build(), HttpStatus.OK);
        // }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ResponseEntity<?> resetPassword(ResetPasswordRequest resetPasswordRequest) {
        String email = resetPasswordRequest.getEmail().trim().toLowerCase();
        String newPassword = resetPasswordRequest.getPassword();
        String confirmPassword = resetPasswordRequest.getConfirmPassword();

        if (!newPassword.equals(confirmPassword)) {
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("Password and confirm password do not match").build(), HttpStatus.BAD_REQUEST);
        }
        try {
            Employee employee = employeeRepository.findByEmail(email).orElseThrow(
                    ResourceNotFoundException::new
            );
            employee.setPassword(passwordEncoder.encode(newPassword));
            employeeRepository.save(employee);
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("Password has been reset successfully").build(), HttpStatus.OK);
        } catch (ResourceNotFoundException ex) {
            log.info("employee with email {} not found in the database", email);
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("employee does not exist with this email").build(), HttpStatus.NOT_FOUND);
        }
    }

    //---------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public ResponseEntity<?> myProfile(ForgotPasswordRequest forgotPasswordRequest) {
        String email = forgotPasswordRequest.getEmail().trim().toLowerCase();
        try {
            Employee employee = employeeRepository.findByEmail(email).orElseThrow(
                    ResourceNotFoundException::new
            );
            return new ResponseEntity<>(UserProfile.builder()
                    .id(employee.getId())
                    .firstName(employee.getName().getFirstName())
                    .lastName(employee.getName().getLastName())
                    .email(employee.getEmail())
                    .phoneNumber(employee.getPhoneNumber())
                    .gender(employee.getGender())
                    .role(employee.getRole())
                    .profilePicture(employee.getProfilePicture())
                    .isOfficiallyEnabled(employee.getIsVerified())
                    .build(), HttpStatus.OK);

        } catch (ResourceNotFoundException ex) {
            log.info("employee with email {} not found in the Database", email);
            return new ResponseEntity<>(GeneralAPIResponse.builder().message("employee does not exist with this email").build(), HttpStatus.NOT_FOUND);
        }
    }
}
