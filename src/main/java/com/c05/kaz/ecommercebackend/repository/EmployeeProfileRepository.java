package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {
}
