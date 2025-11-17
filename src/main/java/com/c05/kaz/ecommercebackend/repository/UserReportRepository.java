package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.UserReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
}
