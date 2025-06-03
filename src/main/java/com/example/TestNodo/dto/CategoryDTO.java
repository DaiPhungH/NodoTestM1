package com.example.TestNodo.dto;

import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public class CategoryDTO {
    private Long id;

    @NotBlank(message = "{category.name.notblank}")
    @Size(max = 100, message = "{category.name.size}")
    private String name;

    @NotBlank(message = "{category.code.notblank}")
    @Size(max = 50, message = "{category.code.size}")
    private String categoryCode;

    @Size(max = 200, message = "{category.description.size}")
    private String description;

    @Pattern(regexp = "[0-1]", message = "{category.status.pattern}")
    private String status = "1";

    private LocalDateTime createdDate;

    private LocalDateTime modifiedDate;

    private String createdBy;

    private String modifiedBy;

    private List<ImageDTO> images;

    // Getters and Setters
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

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
