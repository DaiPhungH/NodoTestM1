package com.example.TestNodo.mapper;

import com.example.TestNodo.dto.ProductDTO;
import com.example.TestNodo.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "images", ignore = true)
    ProductDTO toDTO(Product product);

    @Mapping(target = "productCategories", ignore = true)
    @Mapping(target = "images", ignore = true)
    Product toEntity(ProductDTO productDTO);
}
