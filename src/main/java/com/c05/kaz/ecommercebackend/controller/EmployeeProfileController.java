package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.user.EmployeeProfileDTO;
import com.c05.kaz.ecommercebackend.services.EmployeeProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/hr/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8081")
public class EmployeeProfileController {

    private final EmployeeProfileService employeeProfileService;

    @PreAuthorize("hasRole('HR')")
    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadMyAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        try {
            // ✅ lấy username từ token
            String username = authentication.getName();
            EmployeeProfileDTO dto = employeeProfileService.updateAvatarByUsername(username, file);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('HR')")
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication authentication) {
        String username = authentication.getName();
        EmployeeProfileDTO dto = employeeProfileService.getProfileByUsername(username);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasRole('HR')")
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            @RequestBody EmployeeProfileDTO request,
            Authentication authentication
    ) {
        try {
            String username = authentication.getName();
            EmployeeProfileDTO updated = employeeProfileService.updateProfileByUsername(username, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
