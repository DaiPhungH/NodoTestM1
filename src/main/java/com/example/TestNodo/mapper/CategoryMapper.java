package com.example.TestNodo.mapper;

import com.example.TestNodo.dto.CategoryDTO;
import com.example.TestNodo.dto.ImageDTO;
import com.example.TestNodo.entity.Category;
import com.example.TestNodo.entity.CategoryImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    @Mapping(target = "images", ignore = true)
    CategoryDTO toDTO(Category entity);

    @Mapping(target = "images", ignore = true)
    Category toEntity(CategoryDTO dto);

    default CategoryDTO toDTOWithImages(Category entity) {
        CategoryDTO dto = toDTO(entity);
        if (entity.getImages() != null) {
            List<ImageDTO> imageDTOs = entity.getImages().stream()
                    .filter(img -> img.getStatus() != null && img.getStatus().equals("1"))
                    .map(img -> {
                        ImageDTO imgDTO = new ImageDTO();
                        imgDTO.setName(img.getName());
                        imgDTO.setUrl(img.getUrl());
                        imgDTO.setUuid(img.getUuid());
                        return imgDTO;
                    }).collect(Collectors.toList());
            dto.setImages(imageDTOs.isEmpty() ? null : imageDTOs);
        }
        return dto;
    }
}
