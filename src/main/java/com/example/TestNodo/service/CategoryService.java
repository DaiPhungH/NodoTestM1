package com.example.TestNodo.service;

import com.example.TestNodo.dto.*;
import com.example.TestNodo.entity.*;
import com.example.TestNodo.mapper.CategoryMapper;
import com.example.TestNodo.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream; // Thêm import
import java.io.IOException; // Thêm import cho IOException
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryImageRepository categoryImageRepository;
    private final CategoryMapper categoryMapper;
    private final MessageSource messageSource;
    private final ImageService imageService;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository, CategoryImageRepository categoryImageRepository,
                           CategoryMapper categoryMapper, MessageSource messageSource, ImageService imageService) {
        this.categoryRepository = categoryRepository;
        this.categoryImageRepository = categoryImageRepository;
        this.categoryMapper = categoryMapper;
        this.messageSource = messageSource;
        this.imageService = imageService;
    }

    @Transactional
    public CategoryDTO createCategory(@Valid CategoryDTO categoryDTO, List<MultipartFile> images) {
        if (categoryRepository.existsByCategoryCode(categoryDTO.getCategoryCode())) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("category.code.exists", null, LocaleContextHolder.getLocale())
            );
        }
        Category category = categoryMapper.toEntity(categoryDTO);
        category.setCreatedDate(LocalDateTime.now());
        category.setCreatedBy("admin");
        category.setStatus("1");

        // Lưu category trước để sinh ID
        try {
            category = categoryRepository.save(category);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save category: " + e.getMessage(), e);
        }

        // Sử dụng ImageService để lưu hình ảnh
        List<CategoryImage> categoryImages = imageService.uploadCategoryImages(images, category);
        category.setImages(categoryImages);

        // Lưu lại category để cập nhật quan hệ
        try {
            categoryRepository.save(category);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save category with images: " + e.getMessage(), e);
        }

        CategoryDTO result = categoryMapper.toDTO(category);
        result.setImages(categoryImages.stream().map(img -> {
            ImageDTO imgDTO = new ImageDTO();
            imgDTO.setName(img.getName());
            imgDTO.setUrl(img.getUrl());
            imgDTO.setUuid(img.getUuid());
            return imgDTO;
        }).collect(Collectors.toList()));
        return result;
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, @Valid CategoryDTO categoryDTO, List<MultipartFile> images) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        messageSource.getMessage("category.not.found", null, LocaleContextHolder.getLocale())
                ));
        if (!category.getCategoryCode().equals(categoryDTO.getCategoryCode()) &&
                categoryRepository.existsByCategoryCode(categoryDTO.getCategoryCode())) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("category.code.exists", null, LocaleContextHolder.getLocale())
            );
        }

        category.setName(categoryDTO.getName());
        category.setCategoryCode(categoryDTO.getCategoryCode());
        category.setDescription(categoryDTO.getDescription());
        category.setStatus(categoryDTO.getStatus());
        category.setModifiedDate(LocalDateTime.now());
        category.setModifiedBy("admin");

        // Sử dụng ImageService để cập nhật hình ảnh
        imageService.updateCategoryImages(images, category);

        // Lưu category
        try {
            categoryRepository.save(category);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update category: " + e.getMessage(), e);
        }

        CategoryDTO result = categoryMapper.toDTO(category);
        result.setImages(category.getImages().stream()
                .filter(img -> img.getStatus() != null && img.getStatus().equals("1"))
                .map(img -> {
                    ImageDTO imgDTO = new ImageDTO();
                    imgDTO.setName(img.getName());
                    imgDTO.setUrl(img.getUrl());
                    imgDTO.setUuid(img.getUuid());
                    return imgDTO;
                }).collect(Collectors.toList()));
        return result;
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        messageSource.getMessage("category.not.found", null, LocaleContextHolder.getLocale())
                ));
        category.setStatus("0");
        category.setModifiedDate(LocalDateTime.now());
        category.setModifiedBy("admin");
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<CategoryDTO> searchCategories(String name, String categoryCode, LocalDateTime createdFrom,
                                                            LocalDateTime createdTo, int page, int size) {
        Page<Category> categoryPage = categoryRepository.search(name, categoryCode, createdFrom, createdTo,
                PageRequest.of(page, size));
        List<CategoryDTO> categoryDTOs = categoryPage.getContent().stream().map(category -> {
            CategoryDTO dto = categoryMapper.toDTO(category);
            List<CategoryImage> images = category.getImages();
            if (images != null) {
                dto.setImages(images.stream()
                        .filter(img -> img.getStatus() != null && img.getStatus().equals("1"))
                        .map(img -> {
                            ImageDTO imgDTO = new ImageDTO();
                            imgDTO.setName(img.getName());
                            imgDTO.setUrl(img.getUrl());
                            imgDTO.setUuid(img.getUuid());
                            return imgDTO;
                        }).collect(Collectors.toList()));
            } else {
                dto.setImages(List.of());
            }
            return dto;
        }).collect(Collectors.toList());

        Pagination pagination = new Pagination();
        pagination.setCurrentPage(categoryPage.getNumber());
        pagination.setPageSize(categoryPage.getSize());
        pagination.setTotalElements(categoryPage.getTotalElements());
        pagination.setTotalPages(categoryPage.getTotalPages());
        pagination.setHasNext(categoryPage.hasNext());
        pagination.setHasPrevious(categoryPage.hasPrevious());

        PaginationResponse<CategoryDTO> response = new PaginationResponse<>();
        response.setData(categoryDTOs);
        response.setPagination(pagination);
        return response;
    }

    @Transactional(readOnly = true)
    public byte[] exportCategoriesToExcel(String name, String categoryCode, LocalDateTime createdFrom, LocalDateTime createdTo, String lang) throws Exception {
        Locale locale = (lang != null && (lang.equals("en") || lang.equals("vi"))) ? new Locale(lang) : LocaleContextHolder.getLocale();

        Page<Category> categoryPage = categoryRepository.search(name, categoryCode, createdFrom, createdTo,
                PageRequest.of(0, Integer.MAX_VALUE));
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Categories");

        // Header
        Row headerRow = sheet.createRow(0);
        String[] columns = {
                messageSource.getMessage("excel.header.id", null, locale),
                messageSource.getMessage("excel.header.name", null, locale),
                messageSource.getMessage("excel.header.code", null, locale),
                messageSource.getMessage("excel.header.description", null, locale),
                messageSource.getMessage("excel.header.createdDate", null, locale),
                messageSource.getMessage("excel.header.modifiedDate", null, locale),
                messageSource.getMessage("excel.header.createdBy", null, locale),
                messageSource.getMessage("excel.header.modifiedBy", null, locale)
        };
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Data
        int rowNum = 1;
        for (Category category : categoryPage.getContent()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(category.getId());
            row.createCell(1).setCellValue(category.getName());
            row.createCell(2).setCellValue(category.getCategoryCode());
            row.createCell(3).setCellValue(category.getDescription());
            row.createCell(4).setCellValue(category.getCreatedDate().toString());
            row.createCell(5).setCellValue(category.getModifiedDate() != null ? category.getModifiedDate().toString() : "");
            row.createCell(6).setCellValue(category.getCreatedBy());
            row.createCell(7).setCellValue(category.getModifiedBy() != null ? category.getModifiedBy() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write workbook to ByteArrayOutputStream: " + e.getMessage(), e);
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                // Log error if needed
            }
        }
        return out.toByteArray();
    }

    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        messageSource.getMessage("category.not.found", null, LocaleContextHolder.getLocale())
                ));
        CategoryDTO dto = categoryMapper.toDTO(category);
        List<CategoryImage> images = category.getImages();
        if (images != null) {
            dto.setImages(images.stream()
                    .filter(img -> img.getStatus() != null && img.getStatus().equals("1"))
                    .map(img -> {
                        ImageDTO imgDTO = new ImageDTO();
                        imgDTO.setName(img.getName());
                        imgDTO.setUrl(img.getUrl());
                        imgDTO.setUuid(img.getUuid());
                        return imgDTO;
                    }).collect(Collectors.toList()));
        } else {
            dto.setImages(List.of());
        }
        return dto;
    }
}
