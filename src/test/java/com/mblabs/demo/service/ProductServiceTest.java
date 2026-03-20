package com.mblabs.demo.service;

import com.mblabs.demo.domain.Product;
import com.mblabs.demo.dto.CreateProductRequest;
import com.mblabs.demo.dto.ProductResponse;
import com.mblabs.demo.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductNotificationService notificationService;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProduct() {
        var request = new CreateProductRequest("Notebook", "Dell XPS", new BigDecimal("5000.00"), "Eletronicos");
        var savedProduct = new Product("Notebook", "Dell XPS", new BigDecimal("5000.00"), "Eletronicos");

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        var response = productService.create(request);

        assertThat(response.name()).isEqualTo("Notebook");
        assertThat(response.category()).isEqualTo("Eletronicos");
        assertThat(response.active()).isTrue();
        verify(notificationService).notifyProductCreated(any());
    }

    @Test
    void shouldReturnAllActiveProducts() {
        var products = List.of(
                new Product("Mouse", "Sem fio", new BigDecimal("150.00"), "Perifericos"),
                new Product("Teclado", "Mecanico", new BigDecimal("300.00"), "Perifericos"));

        when(productRepository.findByActiveTrue()).thenReturn(products);

        var result = productService.findAllActive();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(ProductResponse::active);
    }

    @Test
    void shouldReturnProductsByCategory() {
        var products = List.of(
                new Product("Monitor", "4K", new BigDecimal("2000.00"), "Eletronicos"));

        when(productRepository.findByCategoryAndActiveTrue("Eletronicos")).thenReturn(products);

        var result = productService.findByCategory("Eletronicos");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("Eletronicos");
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}
