package com.example.TestNodo.service;

import com.example.TestNodo.dto.ImageDTO;
import com.example.TestNodo.dto.Pagination;
import com.example.TestNodo.dto.PaginationResponse;
import com.example.TestNodo.dto.ProductDTO;
import com.example.TestNodo.entity.Category;
import com.example.TestNodo.entity.Product;
import com.example.TestNodo.entity.ProductCategory;
import com.example.TestNodo.entity.ProductImage;
import com.example.TestNodo.mapper.CategoryMapper;
import com.example.TestNodo.mapper.ProductMapper;
import com.example.TestNodo.repository.CategoryRepository;
import com.example.TestNodo.repository.ProductCategoryRepository;
import com.example.TestNodo.repository.ProductImageRepository;
import com.example.TestNodo.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final MessageSource messageSource;

    @Autowired
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          ProductCategoryRepository productCategoryRepository, ProductImageRepository productImageRepository,
                          ProductMapper productMapper, CategoryMapper categoryMapper, MessageSource messageSource) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productImageRepository = productImageRepository;
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.messageSource = messageSource;
    }

    @Transactional
    public ProductDTO createProduct(@Valid ProductDTO productDTO, List<MultipartFile> images, List<Long> categoryIds) {
        if (productRepository.existsByProductCode(productDTO.getProductCode())) {
            throw new IllegalArgumentException(messageSource.getMessage("product.code.exists", null, LocaleContextHolder.getLocale()));
        }
        Product product = productMapper.toEntity(productDTO);
        product.setCreatedDate(LocalDateTime.now());
        product.setCreatedBy("admin");
        product.setStatus("1");

        // Handle images
        List<ProductImage> productImages = images.stream().map(file -> {
            ProductImage image = new ProductImage();
            image.setName(file.getOriginalFilename());
            image.setUrl("/images/products/" + UUID.randomUUID() + file.getOriginalFilename());
            image.setUuid(UUID.randomUUID().toString());
            image.setStatus("1");
            image.setProduct(product);
            return image;
        }).collect(Collectors.toList());
        product.setImages(productImages);

        // Handle categories
        List<ProductCategory> productCategories = categoryIds.stream().map(categoryId -> {
            Category category = categoryRepository.findByIdWithImages(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("category.not.found", null, LocaleContextHolder.getLocale())));
            ProductCategory pc = new ProductCategory();
            pc.setProduct(product);
            pc.setCategory(category);
            pc.setCreatedDate(LocalDateTime.now());
            pc.setStatus("1");
            return pc;
        }).collect(Collectors.toList());
        product.setProductCategories(productCategories);

        productRepository.save(product);

        return toProductDTO(product);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, @Valid ProductDTO productDTO, List<MultipartFile> images, List<Long> categoryIds) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("product.not.found", null, LocaleContextHolder.getLocale())));
        if (!product.getProductCode().equals(productDTO.getProductCode()) && productRepository.existsByProductCode(productDTO.getProductCode())) {
            throw new IllegalArgumentException(messageSource.getMessage("product.code.exists", null, LocaleContextHolder.getLocale()));
        }

        product.setName(productDTO.getName());
        product.setProductCode(productDTO.getProductCode());
        product.setDescription(productDTO.getDescription());
        product.setStatus(productDTO.getStatus());
        product.setPrice(productDTO.getPrice());
        product.setQuantity(productDTO.getQuantity());
        product.setModifiedDate(LocalDateTime.now());
        product.setModifiedBy("admin");

        // Update images
        if (images != null && !images.isEmpty()) {
            product.getImages().forEach(img -> img.setStatus("0"));
            List<ProductImage> newImages = images.stream().map(file -> {
                ProductImage image = new ProductImage();
                image.setName(file.getOriginalFilename());
                image.setUrl("/images/products/" + UUID.randomUUID() + file.getOriginalFilename());
                image.setUuid(UUID.randomUUID().toString());
                image.setStatus("1");
                image.setProduct(product);
                return image;
            }).collect(Collectors.toList());
            product.getImages().addAll(newImages);
        }

        // Update categories
        if (categoryIds != null) {
            product.getProductCategories().forEach(pc -> pc.setStatus("0"));
            categoryIds.forEach(categoryId -> {
                Category category = categoryRepository.findByIdWithImages(categoryId)
                        .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("category.not.found", null, LocaleContextHolder.getLocale())));
                ProductCategory existing = product.getProductCategories().stream()
                        .filter(pc -> pc.getStatus().equals("0") && pc.getCategory().getId().equals(categoryId))
                        .findFirst().orElse(null);
                if (existing != null) {
                    existing.setStatus("1");
                    existing.setModifiedDate(LocalDateTime.now());
                } else {
                    ProductCategory pc = new ProductCategory();
                    pc.setProduct(product);
                    pc.setCategory(category);
                    pc.setCreatedDate(LocalDateTime.now());
                    pc.setStatus("1");
                    product.getProductCategories().add(pc);
                }
            });
        }

        productRepository.save(product);

        return toProductDTO(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("product.not.found", null, LocaleContextHolder.getLocale())));
        product.setStatus("0");
        product.setModifiedDate(LocalDateTime.now());
        product.setModifiedBy("admin");
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProductDTO> searchProducts(String name, String productCode, LocalDateTime createdFrom,
                                                         LocalDateTime createdTo, Long categoryId, int page, int size) {
        Page<Product> productPage = productRepository.search(name, productCode, createdFrom, createdTo, categoryId, PageRequest.of(page, size));
        List<ProductDTO> productDTOs = productPage.getContent().stream().map(this::toProductDTO).collect(Collectors.toList());

        Pagination pagination = new Pagination();
        pagination.setCurrentPage(productPage.getNumber());
        pagination.setPageSize(productPage.getSize());
        pagination.setTotalElements(productPage.getTotalElements());
        pagination.setTotalPages(productPage.getTotalPages());
        pagination.setHasNext(productPage.hasNext());
        pagination.setHasPrevious(productPage.hasPrevious());

        PaginationResponse<ProductDTO> response = new PaginationResponse<>();
        response.setData(productDTOs);
        response.setPagination(pagination);
        return response;
    }

    @Transactional(readOnly = true)
    public byte[] exportProductsToExcel(String name, String productCode, LocalDateTime createdFrom, LocalDateTime createdTo,
                                        Long categoryId, String lang) throws IOException {
        Locale locale = (lang != null && (lang.equals("en") || lang.equals("vi"))) ? new Locale(lang) : LocaleContextHolder.getLocale();
        Page<Product> productPage = productRepository.search(name, productCode, createdFrom, createdTo, categoryId, PageRequest.of(0, Integer.MAX_VALUE));
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        Row headerRow = sheet.createRow(0);
        String[] columns = {
                messageSource.getMessage("excel.header.id", null, locale),
                messageSource.getMessage("excel.header.name", null, locale),
                messageSource.getMessage("excel.header.code", null, locale),
                messageSource.getMessage("excel.header.price", null, locale),
                messageSource.getMessage("excel.header.quantity", null, locale),
                messageSource.getMessage("excel.header.createdDate", null, locale),
                messageSource.getMessage("excel.header.modifiedDate", null, locale),
                messageSource.getMessage("excel.header.categories", null, locale)
        };
        for (int i = 0; i < columns.length; i++) {
            headerRow.createCell(i).setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (Product product : productPage.getContent()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(product.getId());
            row.createCell(1).setCellValue(product.getName());
            row.createCell(2).setCellValue(product.getProductCode());
            row.createCell(3).setCellValue(product.getPrice());
            row.createCell(4).setCellValue(product.getQuantity());
            row.createCell(5).setCellValue(product.getCreatedDate().toString());
            row.createCell(6).setCellValue(product.getModifiedDate() != null ? product.getModifiedDate().toString() : "");
            String categories = product.getProductCategories().stream()
                    .filter(pc -> pc.getStatus() != null && pc.getStatus().equals("1"))
                    .map(pc -> pc.getCategory().getName())
                    .collect(Collectors.joining(", "));
            row.createCell(7).setCellValue(categories);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } finally {
            workbook.close();
        }
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::toProductDTO)
                .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("product.not.found", null, LocaleContextHolder.getLocale())));
    }

    private ProductDTO toProductDTO(Product product) {
        ProductDTO dto = productMapper.toDTO(product);
        dto.setImages(product.getImages().stream()
                .filter(img -> img.getStatus() != null && img.getStatus().equals("1"))
                .map(img -> {
                    ImageDTO imgDTO = new ImageDTO();
                    imgDTO.setName(img.getName());
                    imgDTO.setUrl(img.getUrl());
                    imgDTO.setUuid(img.getUuid());
                    return imgDTO;
                }).collect(Collectors.toList()));
        dto.setCategories(product.getProductCategories().stream()
                .filter(pc -> pc.getStatus() != null && pc.getStatus().equals("1"))
                .map(pc -> {
                    Category category = categoryRepository.findByIdWithImages(pc.getCategory().getId())
                            .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("category.not.found", null, LocaleContextHolder.getLocale())));
                    return categoryMapper.toDTOWithImages(category); // Sử dụng toDTOWithImages
                }).collect(Collectors.toList()));
        return dto;
    }
}
