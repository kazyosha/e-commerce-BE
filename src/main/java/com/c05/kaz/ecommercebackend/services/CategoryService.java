package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.Category;
import com.c05.kaz.ecommercebackend.exception.ResourceNotFoundException;
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

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy danh mục với id = " + id));
    }

    public Category create(Category request) {

        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .build();

        return categoryRepository.save(category);
    }

    public Category update(Long id, Category request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy danh mục với id = " + id));

        // Nếu đổi tên → check trùng
        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        return categoryRepository.save(category);
    }
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy danh mục với id = " + id));

        // Nếu bro muốn chặn khi có Product → bật đoạn này
         if (category.getProducts() != null && !category.getProducts().isEmpty()) {
             throw new IllegalStateException("Không thể xóa danh mục vì đang có sản phẩm liên quan");
         }

        categoryRepository.delete(category);
    }
}
