package com.tradeflow.tradeflow_product_service.service;

import com.tradeflow.tradeflow_product_service.dto.ProductDto;
import com.tradeflow.tradeflow_product_service.dto.ProductResponse;
import com.tradeflow.tradeflow_product_service.entity.Product;
import com.tradeflow.tradeflow_product_service.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    public final ProductRepository productRepository;
    @Autowired
    public ModelMapper modelMapper;

    public ProductService(ProductRepository productRepository){
        this.productRepository=productRepository;
    }

    public ProductResponse createProduct(ProductDto productDto){
        Product product= modelMapper.map(productDto,Product.class);
        Product savedProduct=productRepository.save(product);
        return modelMapper.map(savedProduct,ProductResponse.class);
    }

    public ProductResponse findProduct(Long id){
        Product product= productRepository.findById(id).get();
        return modelMapper.map(product,ProductResponse.class);
    }

    public List<ProductResponse> getAll(){
        List<Product> list=productRepository.findAll();
        return list.stream().map(e->modelMapper.map(e,ProductResponse.class)).toList();
    }

    public ProductResponse updateProduct(Long id,ProductDto productDto){
        Product product=productRepository.findById(id).get();
        product.setName(productDto.getName());
        product.setCategory(productDto.getCategory());
        product.setPrice(productDto.getPrice());
        product.setStockQuantity(productDto.getStockQuantity());
        Product savedProduct=productRepository.save(product);
        return modelMapper.map(savedProduct,ProductResponse.class);

    }
    public ProductResponse updateProduct(Long id,Integer stock){
        Product product=productRepository.findById(id).get();
        product.setStockQuantity(stock);
        Product savedProduct= productRepository.save(product);
        return modelMapper.map(savedProduct,ProductResponse.class);
    }

}
