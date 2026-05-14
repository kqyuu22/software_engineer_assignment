package com.se.sebtl.controller;

import com.se.sebtl.model.AppUser;
import com.se.sebtl.model.Role;
import com.se.sebtl.repository.AppUserRepository;
import com.se.sebtl.repository.UnimemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AppUserRepository userRepository;
    private final UnimemberRepository unimemberRepository;

    public AuthController(AppUserRepository userRepository, UnimemberRepository unimemberRepository) {
        this.userRepository = userRepository;
        this.unimemberRepository = unimemberRepository;
    }

    @GetMapping("/")
    public String showLoginPage() {
        return "forward:/auth.html";
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<AppUser> userOpt = userRepository.findByUsername(request.getUsername());

        // Validate user exists and password matches
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(request.getPassword())) {
            AppUser user = userOpt.get();
            
            // Generate a simulated JWT token (In production, use io.jsonwebtoken library)
            String expiryIso = java.time.OffsetDateTime.now().plusMinutes(10).toString();
            String fakeJwtToken = "Bearer " + user.getUserId() + "; Expr " + expiryIso; // Token valid for 5 minutes for testing

            System.out.println("[AuthController] User " + user.getUsername() + " logged in with user role: " + user.getRole() + " and name: " + user.getName());
            
            // Make a join query with uni member table to get the role in the university
            Optional<Role> roleInUni = unimemberRepository.findRoleByUserId(user.getUserId());
            if (roleInUni.isPresent()) {
                System.out.println("[AuthController] User " + user.getUsername() + " has university role: " + roleInUni.get());
            } else {
                roleInUni = Optional.of(Role.OTHER); // Default to OTHER if no role found
                System.out.println("[AuthController] User " + user.getUsername() + " has no university role found.");
            }
            
            System.out.println("[AuthController] User " + user.getUsername() + " has university role: " + roleInUni.orElse(null));
            return ResponseEntity.ok(new AuthResponse(fakeJwtToken, user.getRole().toString(), user.getName(), roleInUni.orElse(null).toString()));
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
    public String name;
    public String unirole;
    public AuthResponse(String token, String role, String name, String unirole) { this.token = token; this.role = role; this.name = name; this.unirole = unirole; }
}

class ErrorResponse {
    public String error;
    public ErrorResponse(String error) { this.error = error; }
}