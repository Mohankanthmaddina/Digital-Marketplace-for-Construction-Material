package com.example.buildpro.controller;

import com.example.buildpro.model.Product;
import com.example.buildpro.dto.ProductDTO;
import com.example.buildpro.model.Category;
import com.example.buildpro.service.CartService;
import com.example.buildpro.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.example.buildpro.model.User;
import com.example.buildpro.service.UserService;
import java.util.Map;

@Controller
@RequestMapping("/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @GetMapping("/view")
    public String productsPage(Model model, Principal principal, @RequestParam(required = false) String category) {
        System.out.println("=== PRODUCTS PAGE REQUEST ===");
        System.out.println("Category parameter: " + category);

        List<Product> products;
        List<Category> categories = productService.getAllCategories();

        if (category != null && !category.isEmpty()) {
            System.out.println("Filtering products by category: " + category);
            products = productService.getProductsByCategory(category);
            System.out.println("Found " + products.size() + " products in category: " + category);
            model.addAttribute("selectedCategory", category);
        } else {
            System.out.println("Loading all products");
            products = productService.getAllProducts();
            System.out.println("Found " + products.size() + " total products");
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);

        if (principal != null) {
            String email = principal.getName(); // Spring Security provides this
            model.addAttribute("currentUserEmail", email);

            Optional<User> userOpt = userService.findByEmail(email);
            int cartSize = userOpt.map(u -> cartService.getCartByUser(u).getCartItems().size()).orElse(0);
            model.addAttribute("cartSize", cartSize);
        }

        return "products";
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/view/{id}")
    public String productDetailPage(@PathVariable Long id, Model model) {
        System.out.println(id);
        Optional<Product> product = productService.getProductById(id);
        if (product.isPresent()) {
            model.addAttribute("product", product.get());
            return "product-detail"; // this will render product-detail.html
        } else {
            return "error/404"; // make a simple error page or redirect
        }
    }

    @GetMapping("/category/{categoryName}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String categoryName) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryName));
    }

    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<Product>> getProductsByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(productService.getProductsByBrand(brand));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String q) {
        List<Product> products = productService.searchProducts(q);
        List<ProductDTO> productDTOs = products.stream()
                .map(product -> {
                    ProductDTO dto = new ProductDTO();
                    dto.setId(product.getId());
                    dto.setName(product.getName());
                    dto.setBrand(product.getBrand());
                    dto.setPrice(product.getPrice());
                    dto.setDescription(product.getDescription());
                    dto.setStockQuantity(product.getStockQuantity());
                    dto.setImageUrl(product.getImageUrl());
                    dto.setSpecifications(product.getSpecifications());
                    dto.setCategoryName(
                            product.getCategory() != null ? product.getCategory().getName() : "No Category");
                    dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(productDTOs);
    }

    @GetMapping("/search-results")
    public String searchResultsPage(@RequestParam String q, Model model, Principal principal) {
        System.out.println("=== SEARCH RESULTS PAGE REQUEST ===");
        System.out.println("Search query: " + q);

        List<Product> products = productService.searchProducts(q);
        System.out.println("Found " + products.size() + " products for query: " + q);

        model.addAttribute("products", products);
        model.addAttribute("query", q);

        if (principal != null) {
            String email = principal.getName();
            model.addAttribute("currentUserEmail", email);

            Optional<User> userOpt = userService.findByEmail(email);
            int cartSize = userOpt.map(u -> cartService.getCartByUser(u).getCartItems().size()).orElse(0);
            model.addAttribute("cartSize", cartSize);
        }

        return "search-results";
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }

    // Add to cart endpoint
    @PostMapping("/add/{productId}")
    @ResponseBody
    public ResponseEntity<String> addToCart(@PathVariable Long productId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            Long userId = Long.valueOf(requestBody.get("userId").toString());

            // Get user
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Add to cart using cart service
            cartService.addToCart(userOpt.get(), productId, 1);

            return ResponseEntity.ok("Product added to cart successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error adding product to cart: " + e.getMessage());
        }
    }
}
