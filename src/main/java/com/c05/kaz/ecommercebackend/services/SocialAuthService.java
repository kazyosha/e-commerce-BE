package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.Role;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SocialProvider;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.RoleRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import com.c05.kaz.ecommercebackend.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserAccountRepository userRepo;
    private final RoleRepository roleRepo;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==============================
    // GOOGLE LOGIN (giữ nguyên bản bạn đã dùng)
    // ==============================

    public Map<String, Object> loginWithGoogle(String idTokenString) {
        try {
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idTokenString;
            String json = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(json);

            if (node.has("error_description")) {
                throw new RuntimeException("Google token không hợp lệ: " + node.get("error_description").asText());
            }

            String email = node.path("email").asText();
            String name = node.path("name").asText(email);
            boolean emailVerified = node.path("email_verified").asBoolean(false);
            String sub = node.path("sub").asText();

            if (!emailVerified) {
                throw new RuntimeException("Email Google chưa được xác minh");
            }

            return processSocialUser(SocialProvider.GOOGLE, email, name, sub);

        } catch (Exception e) {
            throw new RuntimeException("Xác thực Google thất bại: " + e.getMessage(), e);
        }
    }

    // ==============================
    // FACEBOOK LOGIN (chuẩn hóa xác thực)
    // ==============================

    public Map<String, Object> loginWithFacebook(String accessToken) {
        try {
            // 1️⃣ Gọi API Graph để xác thực accessToken và lấy user info
            String url = "https://graph.facebook.com/me"
                    + "?fields=id,name,email,picture"
                    + "&access_token=" + accessToken;

            String json = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(json);

            // 2️⃣ Kiểm tra hợp lệ
            if (node.has("error")) {
                String msg = node.path("error").path("message").asText("Không thể xác thực token Facebook");
                throw new RuntimeException(msg);
            }

            // 3️⃣ Lấy thông tin user
            String id = node.path("id").asText(null);
            String email = node.path("email").asText(null);
            String name = node.path("name").asText(email != null ? email : "Facebook User");
            String picture = node.path("picture").path("data").path("url").asText(null);

            if (id == null) {
                throw new RuntimeException("Không lấy được thông tin Facebook user");
            }

            // Nếu không có email, tạo email ảo để duy trì unique constraint
            if (email == null || email.isBlank()) {
                email = "fb_" + id + "@facebook.local";
            }

            // 4️⃣ Xử lý đăng nhập hoặc tạo mới
            Map<String, Object> result = processSocialUser(SocialProvider.FACEBOOK, email, name, id);

            // Thêm ảnh đại diện (nếu có)
            if (picture != null) {
                result.put("avatar", picture);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Xác thực Facebook thất bại: " + e.getMessage(), e);
        }
    }

    // ==============================
    // DÙNG CHUNG (Google & Facebook)
    // ==============================

    private Map<String, Object> processSocialUser(
            SocialProvider provider,
            String email,
            String name,
            String providerId
    ) {
        if (email == null || email.length() > 255) {
            throw new RuntimeException(provider + " không trả về email hợp lệ");
        }

        UserAccount user = userRepo.findByEmail(email)
                .orElseGet(() -> createNewSocialUser(email, name, provider, providerId));

        if (user.getStatus() == AccountStatus.BLOCKED) {
            throw new RuntimeException("Tài khoản đã bị khoá");
        }

        String jwt = jwtService.generateToken(user.getUsername());

        return Map.of(
                "token", jwt,
                "username", user.getUsername(),
                "email", user.getEmail(),
                "userType", user.getUserType().name(),
                "emailVerified", user.isEmailVerified(),
                "provider", user.getProvider().name()
        );
    }

    private UserAccount createNewSocialUser(
            String email,
            String name,
            SocialProvider provider,
            String providerId
    ) {
        String baseUsername = email.split("@")[0];

        UserAccount user = new UserAccount();
        user.setUsername(baseUsername);
        user.setEmail(email);
        user.setPassword(""); // social login -> không cần password
        user.setUserType(UserType.CUSTOMER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setProvider(provider);
        user.setProviderId(providerId);

        Role customerRole = roleRepo.findByCode("CUSTOMER");
        user.setRoles(Collections.singleton(customerRole));

        return userRepo.save(user);
    }
}