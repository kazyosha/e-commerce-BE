package com.c05.kaz.ecommercebackend.repository;


import com.c05.kaz.ecommercebackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
