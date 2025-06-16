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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private final ProductImageRepository productImageRepository;
    private final CategoryImageRepository categoryImageRepository;
    private static final String PRODUCT_UPLOAD_DIR = "src/main/resources/static/images/products/";
    private static final String CATEGORY_UPLOAD_DIR = "src/main/resources/static/images/categories/";
    private static final String PRODUCT_URL_PREFIX = "/images/products/";
    private static final String CATEGORY_URL_PREFIX = "/images/categories/";

    @Autowired
    public ImageService(ProductImageRepository productImageRepository, CategoryImageRepository categoryImageRepository) {
        this.productImageRepository = productImageRepository;
        this.categoryImageRepository = categoryImageRepository;
        // Tạo thư mục lưu trữ nếu chưa tồn tại
        createDirectories();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(PRODUCT_UPLOAD_DIR));
            Files.createDirectories(Paths.get(CATEGORY_UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục lưu trữ ảnh: " + e.getMessage());
        }
    }

    public List<ProductImage> uploadProductImages(List<MultipartFile> files, Product product) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<ProductImage> productImages = files.stream().map(file -> {
            try {
                // Tạo tên file duy nhất
                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path filePath = Paths.get(PRODUCT_UPLOAD_DIR + fileName);

                // Lưu file vào thư mục
                Files.write(filePath, file.getBytes());

                ProductImage image = new ProductImage();
                image.setName(file.getOriginalFilename());
                image.setUrl(PRODUCT_URL_PREFIX + fileName);
                image.setUuid(UUID.randomUUID().toString());
                image.setStatus("1");
                image.setProduct(product);
                return image;
            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi lưu ảnh sản phẩm: " + file.getOriginalFilename(), e);
            }
        }).collect(Collectors.toList());
        return productImageRepository.saveAll(productImages);
    }

    public List<CategoryImage> uploadCategoryImages(List<MultipartFile> files, Category category) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<CategoryImage> categoryImages = files.stream().map(file -> {
            try {
                // Tạo tên file duy nhất
                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path filePath = Paths.get(CATEGORY_UPLOAD_DIR + fileName);

                // Lưu file vào thư mục
                Files.write(filePath, file.getBytes());

                CategoryImage image = new CategoryImage();
                image.setName(file.getOriginalFilename());
                image.setUrl(CATEGORY_URL_PREFIX + fileName);
                image.setUuid(UUID.randomUUID().toString());
                image.setStatus("1");
                image.setCategory(category);
                return image;
            } catch (IOException e) {
                throw new RuntimeException("Lỗi khi lưu ảnh danh mục: " + file.getOriginalFilename(), e);
            }
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