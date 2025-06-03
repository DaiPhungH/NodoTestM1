package com.example.TestNodo.controller;

import com.example.TestNodo.dto.CategoryDTO;
import com.example.TestNodo.dto.PaginationResponse;
import com.example.TestNodo.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(
            @RequestPart("category") @Valid CategoryDTO categoryDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        CategoryDTO createdCategory = categoryService.createCategory(categoryDTO, images);
        return ResponseEntity.ok(createdCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @RequestPart("category") @Valid CategoryDTO categoryDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        CategoryDTO updatedCategory = categoryService.updateCategory(id, categoryDTO, images);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<PaginationResponse<CategoryDTO>> searchCategories(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginationResponse<CategoryDTO> response = categoryService.searchCategories(name, categoryCode, createdFrom, createdTo, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        CategoryDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);

    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCategoriesToExcel(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) String lang) throws Exception {
        // Set locale based on lang parameter
        if (lang != null && (lang.equals("en") || lang.equals("vi"))) {
            LocaleContextHolder.setLocale(new Locale(lang));
        }
        byte[] excelData = categoryService.exportCategoriesToExcel(name, categoryCode, createdFrom, createdTo, lang);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", lang != null && lang.equals("vi") ? "danh_muc.xlsx" : "categories.xlsx");
        return ResponseEntity.ok().headers(headers).body(excelData);
    }
}
