package com.c05.kaz.ecommercebackend.config;

import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.DiscountType;
import com.c05.kaz.ecommercebackend.enums.SocialProvider;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

//@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final PromotionRepository promotionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("🚀 DataInitializer started...");

        // Seed roles trước — an toàn và không duplicate
        seedRoles();

        if (userAccountRepository.count() == 0) {
            seedUsersAndProfiles();
            seedCategoriesAndProducts();
            seedPromotions();
        }

        System.out.println("✅ DataInitializer completed.");
    }

    // ================== ROLES ==================

    private void seedRoles() {
        createRoleIfMissing("ADMIN", "Administrator", "System admin");
        createRoleIfMissing("HR", "Human Resource", "Quản lý nhân sự");
        createRoleIfMissing("SUPPLIER", "Nhà cung cấp", "Chủ cửa hàng");
        createRoleIfMissing("CUSTOMER", "Khách hàng", "Người mua");
    }

    private void createRoleIfMissing(String code, String name, String desc) {
        if (roleRepository.findByCode(code) == null) {
            roleRepository.save(
                    Role.builder()
                            .code(code)
                            .name(name)
                            .description(desc)
                            .build()
            );
            System.out.println("🧩 Created missing role: " + code);
        }
    }

    // ================== USERS & PROFILES ==================

    private void seedUsersAndProfiles() {
        System.out.println("👤 Seeding default users...");

        // ===== ADMIN =====
        UserAccount admin = createUserIfMissing(
                "admin",
                "admin@example.com",
                UserType.ADMIN,
                "ADMIN"
        );

        // ===== HR =====
        UserAccount hr = createUserIfMissing(
                "hr01",
                "hr01@example.com",
                UserType.HR,
                "HR"
        );
        if (!employeeProfileRepository.existsById(hr.getId())) {
            employeeProfileRepository.save(
                    EmployeeProfile.builder()
                            .user(hr)
                            .fullName("Nguyễn Thị Hòa")
                            .age(28)
                            .phone("0987654321")
                            .salary(15_000_000L)
                            .address("TP. Hồ Chí Minh")
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        }

        // ===== SUPPLIERS =====
        UserAccount fashionShopUser = createUserIfMissing(
                "fashion01",
                "fashion01@gmail.com",
                UserType.SUPPLIER,
                "SUPPLIER"
        );
        if (!supplierRepository.existsByUser_Id(fashionShopUser.getId())) {
            supplierRepository.save(
                    SupplierShop.builder()
                            .user(fashionShopUser)
                            .shopName("Fashionista Boutique")
                            .description("Shop thời trang nữ phong cách Hàn Quốc, cập nhật mẫu mới mỗi ngày.")
                            .address("Quận 1, TP. Hồ Chí Minh")
                            .avatarUrl("https://placehold.co/100x100?text=Fashionista")
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        }

        UserAccount streetShopUser = createUserIfMissing(
                "street01",
                "street01@gmail.com",
                UserType.SUPPLIER,
                "SUPPLIER"
        );
        if (!supplierRepository.existsByUser_Id(streetShopUser.getId())) {
            supplierRepository.save(
                    SupplierShop.builder()
                            .user(streetShopUser)
                            .shopName("StreetStyle Store")
                            .description("Shop unisex và local brand phong cách năng động, cá tính.")
                            .address("Cầu Giấy, Hà Nội")
                            .avatarUrl("https://placehold.co/100x100?text=StreetStyle")
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        }

        // ===== CUSTOMER =====
        UserAccount customerUser = createUserIfMissing(
                "customer01",
                "customer01@gmail.com",
                UserType.CUSTOMER,
                "CUSTOMER"
        );
        if (!customerRepository.existsByUser_Id(customerUser.getId())) {
            customerRepository.save(
                    CustomerProfile.builder()
                            .user(customerUser)
                            .fullName("Trần Minh Khang")
                            .birthDate(LocalDate.of(2000, 5, 20))
                            .phone("0909123456")
                            .address("Thủ Đức, TP. Hồ Chí Minh")
                            .avatarUrl("https://placehold.co/80x80?text=Khang")
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        }

        System.out.println("✅ Default users & profiles seeded.");
    }

    private UserAccount createUserIfMissing(String username, String email, UserType type, String roleCode) {
        return userAccountRepository.findByEmail(email).orElseGet(() -> {
            Role role = roleRepository.findByCode(roleCode);
            return userAccountRepository.save(
                    UserAccount.builder()
                            .username(username)
                            .email(email)
                            .password(passwordEncoder.encode("123456"))
                            .userType(type)
                            .status(AccountStatus.ACTIVE)
                            .provider(SocialProvider.LOCAL)
                            .roles(Set.of(role))
                            .createdAt(LocalDateTime.now())
                            .build()
            );
        });
    }

    // ================== CATEGORY + PRODUCTS ==================

    private void seedCategoriesAndProducts() {
        if (categoryRepository.count() > 0) return;

        System.out.println("🛍️ Seeding categories and products...");

        Category shirts = categoryRepository.save(new Category("Áo Thun", "Áo thun cotton thoáng mát, phong cách trẻ trung."));
        Category pants = categoryRepository.save(new Category("Quần Jeans", "Quần jeans form chuẩn, co giãn."));
        Category dresses = categoryRepository.save(new Category("Váy & Đầm", "Đầm công sở, đầm dạo phố, cao cấp."));
        Category shoes = categoryRepository.save(new Category("Giày Sneaker", "Giày sneaker năng động, cá tính."));
        Category accessories = categoryRepository.save(new Category("Phụ Kiện", "Túi, nón, mắt kính thời trang."));

        List<SupplierShop> suppliers = supplierRepository.findAll();
        if (suppliers.size() < 2) return;

        SupplierShop fashionShop = suppliers.get(0);
        SupplierShop streetShop = suppliers.get(1);

        Product p1 = productRepository.save(Product.builder()
                .supplier(fashionShop)
                .category(dresses)
                .name("Đầm voan hoa tay phồng")
                .price(550_000L)
                .quantity(20)
                .active(true)
                .soldQuantity(5L)
                .createdAt(LocalDateTime.now())
                .build());

        productImageRepository.save(
                ProductImage.builder().product(p1).imageUrl("https://placehold.co/400x300?text=DamVoan").mainImage(true).build()
        );

        System.out.println("✅ Categories & products seeded.");
    }

    // ================== PROMOTIONS ==================

    private void seedPromotions() {
        if (promotionRepository.count() > 0) return;

        List<SupplierShop> suppliers = supplierRepository.findAll();
        if (suppliers.size() < 2) return;

        SupplierShop fashionShop = suppliers.get(0);
        SupplierShop streetShop = suppliers.get(1);

        promotionRepository.saveAll(List.of(
                Promotion.builder()
                        .supplier(fashionShop)
                        .code("FASHION10")
                        .discountType(DiscountType.PERCENT)
                        .discountValue(10.0)
                        .minOrderValue(300_000L)
                        .maxUsage(100)
                        .usedCount(0)
                        .startAt(LocalDateTime.now())
                        .endAt(LocalDateTime.now().plusMonths(1))
                        .active(true)
                        .build(),
                Promotion.builder()
                        .supplier(streetShop)
                        .code("STREET15")
                        .discountType(DiscountType.PERCENT)
                        .discountValue(15.0)
                        .minOrderValue(500_000L)
                        .maxUsage(200)
                        .usedCount(0)
                        .startAt(LocalDateTime.now())
                        .endAt(LocalDateTime.now().plusMonths(2))
                        .active(true)
                        .build()
        ));

        System.out.println("✅ Promotions seeded.");
    }
}
