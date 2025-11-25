package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import com.c05.kaz.ecommercebackend.entity.Role;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.SocialProvider;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.CustomerRepository;
import com.c05.kaz.ecommercebackend.repository.RoleRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import com.c05.kaz.ecommercebackend.repository.UserAccountRepository;
import com.c05.kaz.ecommercebackend.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserAccountRepository userRepo;
    private final RoleRepository roleRepo;
    private final CustomerRepository customerRepo;
    private final SupplierRepository supplierRepo;
    private final JwtService jwtService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ======================================================
    // 🔥 GOOGLE LOGIN
    // ======================================================
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

    // ======================================================
    // 🔥 FACEBOOK LOGIN
    // ======================================================
    public Map<String, Object> loginWithFacebook(String accessToken) {
        try {
            String url = "https://graph.facebook.com/me"
                    + "?fields=id,name,email,picture"
                    + "&access_token=" + accessToken;

            String json = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(json);

            if (node.has("error")) {
                String msg = node.path("error").path("message").asText("Không thể xác thực token Facebook");
                throw new RuntimeException(msg);
            }

            String id = node.path("id").asText();
            String email = node.path("email").asText(null);
            String name = node.path("name").asText(email != null ? email : "Facebook User");
            String avatarUrl = node.path("picture").path("data").path("url").asText(null);

            if (email == null) {
                email = "fb_" + id + "@facebook.local";
            }

            Map<String, Object> result = processSocialUser(SocialProvider.FACEBOOK, email, name, id);
            result.put("avatar", avatarUrl);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Xác thực Facebook thất bại: " + e.getMessage(), e);
        }
    }

    // ======================================================
    // 🔥 Hàm xử lý chung cho Google + Facebook
    // ======================================================
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

        // Tạo Spring UserDetails để sinh JWT
        var springUser = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password("")  // social login không cần password
                .authorities(user.getRoles().stream()
                        .map(r -> "ROLE_" + r.getCode())
                        .toArray(String[]::new))
                .build();

        // JWT chứa userId
        String token = jwtService.generateToken(springUser, user.getId());

        // ======================================================
        // 🔥 Build response trả về FE (CỰC QUAN TRỌNG)
        // ======================================================
        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("id", user.getId());                     // USER ID
        res.put("email", user.getEmail());
        res.put("username", user.getUsername());
        res.put("userType", user.getUserType().name());
        res.put("provider", user.getProvider().name());

//        // CustomerId nếu là CUSTOMER
//        if (user.getUserType() == UserType.CUSTOMER && user.getCustomer() != null) {
//            res.put("customerId", user.getCustomer().getId());
//        }

        // SupplierId nếu là SUPPLIER
//        if (user.getUserType() == UserType.SUPPLIER && user.getSupplier() != null) {
//            res.put("supplierId", user.getSupplier().getId());
//        }

        return res;
    }

    // ======================================================
    // 🔥 Tạo user mới + CustomerProfile (social login mặc định là CUSTOMER)
    // ======================================================
    private UserAccount createNewSocialUser(
            String email,
            String name,
            SocialProvider provider,
            String providerId
    ) {
        String baseUsername = email.split("@")[0];

        Role customerRole = roleRepo.findByCode("CUSTOMER");

        UserAccount user = new UserAccount();
        user.setUsername(baseUsername);
        user.setEmail(email);
        user.setPassword(""); // social login không dùng password
        user.setUserType(UserType.CUSTOMER);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setRoles(Collections.singleton(customerRole));

        userRepo.save(user);

        // 🔥 Tạo CustomerProfile với MapsId = user.id
        CustomerProfile profile = CustomerProfile.builder()
                .id(user.getId())
                .user(user)
                .fullName(name)
                .build();

        customerRepo.save(profile);

        return user;
    }
}
