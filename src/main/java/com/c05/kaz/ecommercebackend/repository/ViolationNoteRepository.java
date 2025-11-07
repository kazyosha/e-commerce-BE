package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.EmployeeProfile;
import com.c05.kaz.ecommercebackend.entity.ViolationNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationNoteRepository extends JpaRepository<ViolationNote, Long> {
    List<ViolationNote> findByCreatedBy(EmployeeProfile employee);
}