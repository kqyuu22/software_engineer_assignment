package com.se.sebtl.controller;

import com.se.sebtl.model.AppUser;
import com.se.sebtl.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AppUserRepository userRepository;

    public AuthController(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<AppUser> userOpt = userRepository.findByUsername(request.getUsername());

        // Validate user exists and password matches
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(request.getPassword())) {
            AppUser user = userOpt.get();
            
            // Generate a simulated JWT token (In production, use io.jsonwebtoken library)
            String fakeJwtToken = "Bearer " + user.getUserId(); 

            System.out.println("[AuthController] User " + user.getUsername() + " logged in with role: " + user.getRole());
            
            return ResponseEntity.ok(new AuthResponse(fakeJwtToken, user.getRole().toString()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(new ErrorResponse("Invalid username or password"));
    }
}

// DTOs for the JSON payloads
class LoginRequest {
    private String username;
    private String password;
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}

class AuthResponse {
    public String token;
    public String role;
    public AuthResponse(String token, String role) { this.token = token; this.role = role; }
}

class ErrorResponse {
    public String error;
    public ErrorResponse(String error) { this.error = error; }
}