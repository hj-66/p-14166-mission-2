package com.mysite.sbb;

import com.mysite.sbb.category.Category;
import com.mysite.sbb.category.CategoryService;
import com.mysite.sbb.question.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SbbApplicationTests {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private CategoryService categoryService;

    @Test
    void contextLoads() {
        categoryService.create("자유게시판");
        categoryService.create("강의");
    }
}
