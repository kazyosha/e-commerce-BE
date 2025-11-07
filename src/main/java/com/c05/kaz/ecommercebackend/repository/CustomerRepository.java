package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<CustomerProfile, Long> {
}
