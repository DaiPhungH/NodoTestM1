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
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMapper productMapper;
    private final ImageService imageService;
    private final CategoryMapper categoryMapper;
    private final MessageSource messageSource;

    @Autowired
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          ProductCategoryRepository productCategoryRepository, ProductImageRepository productImageRepository,
                          ProductMapper productMapper, CategoryMapper categoryMapper, MessageSource messageSource, ImageService imageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productImageRepository = productImageRepository;
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.messageSource = messageSource;
        this.imageService = imageService;
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

        // Lưu product trước để sinh ID
        try {
            product = productRepository.save(product);
        } catch (Exception e) {
            throw new RuntimeException("Không thể lưu sản phẩm: " + e.getMessage(), e);
        }

        // Sử dụng ImageService để lưu hình ảnh vào thư mục cục bộ và tạo URL
        List<ProductImage> productImages = imageService.uploadProductImages(images, product);
        product.setImages(productImages);

        // Xử lý danh mục
        product.setProductCategories(createProductCategories(product, categoryIds));

        // Lưu lại product để cập nhật quan hệ
        try {
            productRepository.save(product);
        } catch (Exception e) {
            throw new RuntimeException("Không thể lưu sản phẩm với hình ảnh và danh mục: " + e.getMessage(), e);
        }

        return toProductDTO(product);
    }

    private List<ProductCategory> createProductCategories(Product product, List<Long> categoryIds) {
        return categoryIds.stream().map(categoryId -> {
            Category category = categoryRepository.findByIdWithImages(categoryId)
                    .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("category.not.found", null, LocaleContextHolder.getLocale())));
            ProductCategory pc = new ProductCategory();
            pc.setProduct(product);
            pc.setCategory(category);
            pc.setCreatedDate(LocalDateTime.now());
            pc.setStatus("1");
            return pc;
        }).collect(Collectors.toList());
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

        // Cập nhật hình ảnh sử dụng ImageService (đã lưu vào thư mục cục bộ)
        if (images != null && !images.isEmpty()) {
            imageService.updateProductImages(images, product);
        }

        // Cập nhật danh mục
        if (categoryIds != null) {
            updateProductCategories(product, categoryIds);
        }

        // Lưu product
        try {
            productRepository.save(product);
        } catch (Exception e) {
            throw new RuntimeException("Không thể cập nhật sản phẩm: " + e.getMessage(), e);
        }

        return toProductDTO(product);
    }

    private void updateProductCategories(Product product, List<Long> categoryIds) {
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

    private List<ImageDTO> toImageDTOs(List<ProductImage> images) {
        if (images == null) return List.of();
        return images.stream()
                .filter(img -> "1".equals(img.getStatus()))
                .map(img -> {
                    ImageDTO dto = new ImageDTO();
                    dto.setName(img.getName());
                    dto.setUrl(img.getUrl());
                    dto.setUuid(img.getUuid());
                    return dto;
                }).collect(Collectors.toList());
    }

    private ProductDTO toProductDTO(Product product) {
        ProductDTO dto = productMapper.toDTO(product);
        dto.setImages(toImageDTOs(product.getImages()));
        dto.setCategories(product.getProductCategories().stream()
                .filter(pc -> pc.getStatus() != null && pc.getStatus().equals("1"))
                .map(pc -> {
                    Category category = categoryRepository.findByIdWithImages(pc.getCategory().getId())
                            .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("category.not.found", null, LocaleContextHolder.getLocale())));
                    return categoryMapper.toDTOWithImages(category);
                }).collect(Collectors.toList()));
        return dto;
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
}