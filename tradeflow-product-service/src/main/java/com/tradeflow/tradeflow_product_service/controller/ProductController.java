package com.tradeflow.tradeflow_product_service.controller;

import com.tradeflow.tradeflow_product_service.dto.ProductDto;
import com.tradeflow.tradeflow_product_service.dto.ProductResponse;
import com.tradeflow.tradeflow_product_service.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    public final ProductService productService;

    public ProductController(ProductService productService){
        this.productService=productService;
    }

    @PostMapping()
    public ProductResponse createProduct(@RequestBody ProductDto productDto){
        return productService.createProduct(productDto);
    }

    @GetMapping("/{id}")
    public ProductResponse findProduct(@PathVariable Long id){
        return productService.findProduct(id);
    }

    @GetMapping
    public List<ProductResponse> getAllProducts(){
        return productService.getAll();
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto){
        return productService.updateProduct(id,productDto);
    }

    @PatchMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id,@RequestBody Integer stock){
        return productService.updateProduct(id,stock);
    }

}
