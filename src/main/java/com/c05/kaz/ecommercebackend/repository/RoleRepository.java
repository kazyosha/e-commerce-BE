package com.c05.kaz.ecommercebackend.repository;


import com.c05.kaz.ecommercebackend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByCode(String code);
}