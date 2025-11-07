package com.c05.kaz.ecommercebackend.config;

import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.AccountStatus;
import com.c05.kaz.ecommercebackend.enums.DiscountType;
import com.c05.kaz.ecommercebackend.enums.SocialProvider;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Configuration
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

        if (roleRepository.count() == 0) {
            seedRoles();
            System.out.println("✅ Seeded roles");
        }

        if (userAccountRepository.count() == 0) {
            seedUsersAndProfiles();
            System.out.println("✅ Seeded users & profiles");
            seedCategoriesAndProducts();
            System.out.println("✅ Seeded fashion categories & products");
            seedPromotions();
            System.out.println("✅ Seeded promotions");
        }

        System.out.println("✅ DataInitializer completed");
    }

    // ================== ROLES ==================

    private void seedRoles() {
        List<Role> roles = List.of(
                Role.builder()
                        .code("ADMIN")
                        .name("Administrator")
                        .description("System admin")
                        .build(),
                Role.builder()
                        .code("HR")
                        .name("Human Resource")
                        .description("Quản lý nhân sự")
                        .build(),
                Role.builder()
                        .code("SUPPLIER")
                        .name("Nhà cung cấp")
                        .description("Chủ cửa hàng")
                        .build(),
                Role.builder()
                        .code("CUSTOMER")
                        .name("Khách hàng")
                        .description("Người mua")
                        .build()
        );
        roleRepository.saveAll(roles);
    }

    // ================== USERS & PROFILES ==================

    private void seedUsersAndProfiles() {
        // ===== ADMIN =====
        UserAccount admin = userAccountRepository.save(
                UserAccount.builder()
                        .username("admin")
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("123456"))
                        .userType(UserType.ADMIN)
                        .status(AccountStatus.ACTIVE)
                        .provider(SocialProvider.LOCAL)
                        .roles(Set.of(roleRepository.findByCode("ADMIN")))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // ===== HR =====
        UserAccount hr = userAccountRepository.save(
                UserAccount.builder()
                        .username("hr01")
                        .email("hr01@example.com")
                        .password(passwordEncoder.encode("123456"))
                        .userType(UserType.HR)
                        .status(AccountStatus.ACTIVE)
                        .provider(SocialProvider.LOCAL)
                        .roles(Set.of(roleRepository.findByCode("HR")))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

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

        // ===== SUPPLIER 1: Fashionista Boutique =====
        UserAccount fashionShopUser = userAccountRepository.save(
                UserAccount.builder()
                        .username("fashion01")
                        .email("fashion01@gmail.com")
                        .password(passwordEncoder.encode("123456"))
                        .userType(UserType.SUPPLIER)
                        .status(AccountStatus.ACTIVE)
                        .provider(SocialProvider.LOCAL)
                        .roles(Set.of(roleRepository.findByCode("SUPPLIER")))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

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

        // ===== SUPPLIER 2: StreetStyle Store =====
        UserAccount streetShopUser = userAccountRepository.save(
                UserAccount.builder()
                        .username("street01")
                        .email("street01@gmail.com")
                        .password(passwordEncoder.encode("123456"))
                        .userType(UserType.SUPPLIER)
                        .status(AccountStatus.ACTIVE)
                        .provider(SocialProvider.LOCAL)
                        .roles(Set.of(roleRepository.findByCode("SUPPLIER")))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

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

        // ===== CUSTOMER SAMPLE =====
        UserAccount customerUser = userAccountRepository.save(
                UserAccount.builder()
                        .username("customer01")
                        .email("customer01@gmail.com")
                        .password(passwordEncoder.encode("123456"))
                        .userType(UserType.CUSTOMER)
                        .status(AccountStatus.ACTIVE)
                        .provider(SocialProvider.LOCAL)
                        .roles(Set.of(roleRepository.findByCode("CUSTOMER")))
                        .createdAt(LocalDateTime.now())
                        .build()
        );

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

    // ================== FASHION CATEGORIES & PRODUCTS ==================

    private void seedCategoriesAndProducts() {
        // Categories thời trang
        Category shirts = categoryRepository.save(
                Category.builder()
                        .name("Áo Thun")
                        .description("Áo thun nam nữ cotton, thoáng mát, phong cách trẻ trung.")
                        .build()
        );
        Category pants = categoryRepository.save(
                Category.builder()
                        .name("Quần Jeans")
                        .description("Quần jeans nam nữ, form chuẩn, co giãn tốt.")
                        .build()
        );
        Category dresses = categoryRepository.save(
                Category.builder()
                        .name("Váy & Đầm")
                        .description("Đầm công sở, đầm dạo phố, chất liệu cao cấp.")
                        .build()
        );
        Category shoes = categoryRepository.save(
                Category.builder()
                        .name("Giày Sneaker")
                        .description("Giày sneaker thời trang, năng động, cá tính.")
                        .build()
        );
        Category accessories = categoryRepository.save(
                Category.builder()
                        .name("Phụ Kiện")
                        .description("Túi xách, nón, thắt lưng, mắt kính, phụ kiện thời trang.")
                        .build()
        );

        List<SupplierShop> suppliers = supplierRepository.findAll();
        if (suppliers.size() < 2) {
            return; // safety check
        }

        SupplierShop fashionShop = suppliers.get(0);
        SupplierShop streetShop = suppliers.get(1);

        // ===== Sản phẩm Fashionista Boutique =====
        Product p1 = productRepository.save(
                Product.builder()
                        .supplier(fashionShop)
                        .category(dresses)
                        .name("Đầm voan hoa tay phồng")
                        .description("Chất liệu voan cao cấp, form dáng nhẹ nhàng, phù hợp dạo phố & hẹn hò.")
                        .price(550_000L)
                        .quantity(20)
                        .active(true)
                        .soldQuantity(5L)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Product p2 = productRepository.save(
                Product.builder()
                        .supplier(fashionShop)
                        .category(shirts)
                        .name("Áo sơ mi trắng basic công sở")
                        .description("Vải cotton thoáng mát, form basic, dễ phối chân váy/quần tây.")
                        .price(320_000L)
                        .quantity(30)
                        .active(true)
                        .soldQuantity(8L)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // ===== Sản phẩm StreetStyle Store =====
        Product p3 = productRepository.save(
                Product.builder()
                        .supplier(streetShop)
                        .category(shirts)
                        .name("Áo thun unisex LocalBrand StreetStyle")
                        .description("Form rộng, in logo nổi, phù hợp nam nữ, chất cotton 100%.")
                        .price(280_000L)
                        .quantity(50)
                        .active(true)
                        .soldQuantity(15L)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Product p4 = productRepository.save(
                Product.builder()
                        .supplier(streetShop)
                        .category(pants)
                        .name("Quần jeans rách gối unisex")
                        .description("Phong cách Hàn Quốc, jean mềm co giãn, phù hợp đi chơi.")
                        .price(420_000L)
                        .quantity(40)
                        .active(true)
                        .soldQuantity(10L)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Product p5 = productRepository.save(
                Product.builder()
                        .supplier(streetShop)
                        .category(shoes)
                        .name("Giày sneaker trắng cổ thấp StreetStyle")
                        .description("Thiết kế trẻ trung, dễ phối đồ, đế êm, phù hợp đi học/đi làm.")
                        .price(650_000L)
                        .quantity(25)
                        .active(true)
                        .soldQuantity(12L)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Product p6 = productRepository.save(
                Product.builder()
                        .supplier(streetShop)
                        .category(accessories)
                        .name("Mũ bucket StreetStyle logo thêu")
                        .description("Mũ bucket unisex, logo thêu tỉ mỉ, item must-have cho giới trẻ.")
                        .price(180_000L)
                        .quantity(60)
                        .active(true)
                        .soldQuantity(20L)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // Ảnh sản phẩm
        productImageRepository.saveAll(List.of(
                ProductImage.builder()
                        .product(p1)
                        .imageUrl("https://placehold.co/400x300?text=DamVoan")
                        .mainImage(true)
                        .build(),
                ProductImage.builder()
                        .product(p2)
                        .imageUrl("https://placehold.co/400x300?text=AOSoMiTrang")
                        .mainImage(true)
                        .build(),
                ProductImage.builder()
                        .product(p3)
                        .imageUrl("https://placehold.co/400x300?text=AoThunUnisex")
                        .mainImage(true)
                        .build(),
                ProductImage.builder()
                        .product(p4)
                        .imageUrl("https://placehold.co/400x300?text=QuanJeans")
                        .mainImage(true)
                        .build(),
                ProductImage.builder()
                        .product(p5)
                        .imageUrl("https://placehold.co/400x300?text=GiaySneaker")
                        .mainImage(true)
                        .build(),
                ProductImage.builder()
                        .product(p6)
                        .imageUrl("https://placehold.co/400x300?text=MuBucket")
                        .mainImage(true)
                        .build()
        ));
    }

    // ================== PROMOTIONS ==================

    private void seedPromotions() {
        List<SupplierShop> suppliers = supplierRepository.findAll();
        if (suppliers.size() < 2) return;

        SupplierShop fashionShop = suppliers.get(0);
        SupplierShop streetShop = suppliers.get(1);

        Promotion promo1 = Promotion.builder()
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
                .build();

        Promotion promo2 = Promotion.builder()
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
                .build();

        Promotion promo3 = Promotion.builder()
                .supplier(streetShop)
                .code("FREEHAT")
                .discountType(DiscountType.AMOUNT)
                .discountValue(180_000.0)
                .minOrderValue(1_000_000L)
                .maxUsage(50)
                .usedCount(0)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusWeeks(2))
                .active(true)
                .build();

        promotionRepository.saveAll(List.of(promo1, promo2, promo3));
    }
}
