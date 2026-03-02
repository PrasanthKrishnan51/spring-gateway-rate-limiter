package com.pk.ratelimiter.service;

import com.pk.ratelimiter.dto.ProductDto;
import com.pk.ratelimiter.exception.ProductException;
import com.pk.ratelimiter.model.Product;
import com.pk.ratelimiter.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic layer.
 *
 * Cache names:
 *   "product"  – single Product keyed by id
 *   "products" – collections (all, by-category, in-stock …)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    //@Cacheable(value = "products", key = "'all'")
    public List<ProductDto.Response> getAllProducts() {
        log.debug("Cache MISS – fetching all products from MongoDB");
        return productRepository.findAll()
                .stream()
                .map(ProductDto.Response::from)
                .toList();
    }

    @Cacheable(value = "product", key = "#id")
    public ProductDto.Response getProductById(String id) {
        log.debug("Cache MISS – fetching product {} from MongoDB", id);
        return productRepository.findById(id)
                .map(ProductDto.Response::from)
                .orElseThrow(() -> new ProductException.ProductNotFoundException(id));
    }

    @Cacheable(value = "products", key = "'category:' + #category.toLowerCase()")
    public List<ProductDto.Response> getProductsByCategory(String category) {
        log.debug("Cache MISS – fetching products by category '{}' from MongoDB", category);
        return productRepository.findByCategoryIgnoreCase(category)
                .stream()
                .map(ProductDto.Response::from)
                .toList();
    }

    @Cacheable(value = "products", key = "'in-stock'")
    public List<ProductDto.Response> getInStockProducts() {
        log.debug("Cache MISS – fetching in-stock products from MongoDB");
        return productRepository.findInStock()
                .stream()
                .map(ProductDto.Response::from)
                .toList();
    }

    public List<ProductDto.Response> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(ProductDto.Response::from)
                .toList();
    }

    @CacheEvict(value = "products", allEntries = true)
    public ProductDto.Response createProduct(ProductDto.CreateRequest req) {
        if (productRepository.existsBySku(req.sku())) {
            throw new ProductException.DuplicateSkuException(req.sku());
        }
        Product product = Product.builder()
                .name(req.name())
                .description(req.description())
                .sku(req.sku())
                .price(req.price())
                .category(req.category())
                .stock(req.stock() != null ? req.stock() : 0)
                .build();

        Product saved = productRepository.save(product);
        log.debug("Created product {}", saved.getId());
        return ProductDto.Response.from(saved);
    }

    @Caching(
        put   = { @CachePut(value = "product", key = "#id") },
        evict = { @CacheEvict(value = "products", allEntries = true) }
    )
    public ProductDto.Response updateProduct(String id, ProductDto.UpdateRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductException.ProductNotFoundException(id));

        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setCategory(req.category());
        product.setStock(req.stock() != null ? req.stock() : product.getStock());

        Product updated = productRepository.save(product);
        log.debug("Updated product {}", updated.getId());
        return ProductDto.Response.from(updated);
    }

    @Caching(evict = {
        @CacheEvict(value = "product",  key = "#id"),
        @CacheEvict(value = "products", allEntries = true)
    })
    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new ProductException.ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
        log.debug("Deleted product {}", id);
    }
}
