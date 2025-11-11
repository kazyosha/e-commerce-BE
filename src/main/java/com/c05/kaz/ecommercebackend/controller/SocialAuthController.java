package com.c05.kaz.ecommercebackend.controller;
import com.c05.kaz.ecommercebackend.dto.SocialLoginRequest;
import com.c05.kaz.ecommercebackend.services.SocialAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody SocialLoginRequest req) {
        if (req.getToken() == null) {
            return ResponseEntity.badRequest().body("Missing idToken");
        }
        return ResponseEntity.ok(socialAuthService.loginWithGoogle(req.getToken()));
    }

    @PostMapping("/facebook")
    public ResponseEntity<?> facebookLogin(@RequestBody SocialLoginRequest req) {
        if (req.getToken() == null) {
            return ResponseEntity.badRequest().body("Missing accessToken");
        }
        return ResponseEntity.ok(socialAuthService.loginWithFacebook(req.getToken()));
    }
}
