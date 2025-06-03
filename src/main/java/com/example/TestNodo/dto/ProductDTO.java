package com.example.TestNodo.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public class ProductDTO {
    private Long id;

    @NotBlank(message = "{product.name.notblank}")
    @Size(max = 200, message = "{product.name.size}")
    @Column(length = 200)
    private String name;

    @NotBlank(message = "{product.code.notblank}")
    @Size(max = 50, message = "{product.code.size}")
    private String productCode;


    @Size(max = 500, message = "{product.description.size}")
    private String description;

    @NotNull(message = "{product.price.notnull}")
    @PositiveOrZero(message = "{product.price.positive}")
    private Double price;

    @NotNull(message = "{product.quantity.notnull}")
    @PositiveOrZero(message = "{product.quantity.positive}")
    private Long quantity;

    @Pattern(regexp = "[0-1]", message = "{product.status.pattern}")
    private String status = "1";
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String createdBy;
    private String modifiedBy;
    private List<ImageDTO> images;
    private List<CategoryDTO> categories;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public List<ImageDTO> getImages() {
        return images;
    }

    public void setImages(List<ImageDTO> images) {
        this.images = images;
    }

    public List<CategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories;
    }
}
