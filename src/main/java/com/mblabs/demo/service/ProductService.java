package com.mblabs.demo.service;

import com.mblabs.demo.domain.Product;
import com.mblabs.demo.dto.CreateProductRequest;
import com.mblabs.demo.dto.ProductResponse;
import com.mblabs.demo.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductNotificationService notificationService;

    public ProductService(ProductRepository productRepository, ProductNotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        var product = new Product(request.name(), request.description(), request.price(), request.category());
        var saved = productRepository.save(product);
        notificationService.notifyProductCreated(saved);
        return ProductResponse.from(saved);
    }

    public List<ProductResponse> findAllActive() {
        return productRepository.findByActiveTrue()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> findByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category)
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse findById(Long id) {
        return productRepository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
    }

    @Transactional
    public void deactivate(Long id) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        product.deactivate();
    }
}
