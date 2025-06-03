package com.example.TestNodo.service;

import com.example.TestNodo.entity.Category;
import com.example.TestNodo.entity.CategoryImage;
import com.example.TestNodo.entity.Product;
import com.example.TestNodo.entity.ProductImage;
import com.example.TestNodo.repository.CategoryImageRepository;
import com.example.TestNodo.repository.ProductImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private final ProductImageRepository productImageRepository;
    private final CategoryImageRepository categoryImageRepository;

    @Autowired
    public ImageService(ProductImageRepository productImageRepository, CategoryImageRepository categoryImageRepository) {
        this.productImageRepository = productImageRepository;
        this.categoryImageRepository = categoryImageRepository;
    }

    public List<ProductImage> uploadProductImages(List<MultipartFile> files, Product product) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<ProductImage> productImages = files.stream().map(file -> {
            ProductImage image = new ProductImage();
            image.setName(file.getOriginalFilename());
            image.setUrl("/images/products/" + UUID.randomUUID().toString() + file.getOriginalFilename());
            image.setUuid(UUID.randomUUID().toString());
            image.setStatus("1");
            image.setProduct(product);
            return image;
        }).collect(Collectors.toList());
        return productImageRepository.saveAll(productImages);
    }

    public List<CategoryImage> uploadCategoryImages(List<MultipartFile> files, Category category) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<CategoryImage> categoryImages = files.stream().map(file -> {
            CategoryImage image = new CategoryImage();
            image.setName(file.getOriginalFilename());
            image.setUrl("/images/categories/" + UUID.randomUUID().toString() + file.getOriginalFilename());
            image.setUuid(UUID.randomUUID().toString());
            image.setStatus("1");
            image.setCategory(category);
            return image;
        }).collect(Collectors.toList());
        return categoryImageRepository.saveAll(categoryImages);
    }

    public void updateProductImages(List<MultipartFile> files, Product product) {
        if (files != null && !files.isEmpty()) {
            // Đặt trạng thái ảnh cũ thành "0"
            product.getImages().forEach(img -> img.setStatus("0"));
            productImageRepository.saveAll(product.getImages());
            // Thêm ảnh mới
            List<ProductImage> newImages = uploadProductImages(files, product);
            product.getImages().addAll(newImages);
        }
    }

    public void updateCategoryImages(List<MultipartFile> files, Category category) {
        if (files != null && !files.isEmpty()) {
            // Đặt trạng thái ảnh cũ thành "0"
            category.getImages().forEach(img -> img.setStatus("0"));
            categoryImageRepository.saveAll(category.getImages());
            // Thêm ảnh mới
            List<CategoryImage> newImages = uploadCategoryImages(files, category);
            category.getImages().addAll(newImages);
        }
    }
}