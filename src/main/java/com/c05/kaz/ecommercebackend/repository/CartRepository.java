package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.Cart;
import com.c05.kaz.ecommercebackend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomer(UserAccount customer);
}
