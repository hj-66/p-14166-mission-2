package com.mysite.sbb.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public Category create(String name) {
        Category category = new Category();
        category.setName(name);

        return category;
    }

    @Transactional(readOnly = true)
    public List<Category> getList() {
        return categoryRepository.findAll();
    }
}
