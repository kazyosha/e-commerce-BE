package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.Category;
import com.c05.kaz.ecommercebackend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }
}
