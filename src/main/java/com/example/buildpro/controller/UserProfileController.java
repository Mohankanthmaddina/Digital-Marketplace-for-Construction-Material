package com.example.buildpro.controller;

import com.example.buildpro.model.User;
import com.example.buildpro.model.Order;
import com.example.buildpro.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
@CrossOrigin(origins = "*")
public class UserProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private com.example.buildpro.service.OrderService orderService;

    @GetMapping
    public String profilePage(Model model, Principal principal, @RequestParam(required = false) Long userId) {
        Optional<User> userOpt = (userId != null)
                ? userService.findById(userId)
                : (principal != null ? userService.findByEmail(principal.getName()) : Optional.empty());
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found.");
            model.addAttribute("orders", java.util.Collections.emptyList());
            model.addAttribute("orderCount", 0);
            if (userId != null) {
                model.addAttribute("userId", userId);
            }
            return "user-profile";
        }
        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("userId", user.getId());

        // Get user's recent orders
        List<Order> orders = orderService.getUserOrders(user);
        model.addAttribute("orders", orders);
        model.addAttribute("orderCount", orders.size());

        return "user-profile";
    }

    @GetMapping("/orders")
    public String ordersPage(Model model, Principal principal, @RequestParam(required = false) Long userId) {
        Optional<User> userOpt = (userId != null)
                ? userService.findById(userId)
                : (principal != null ? userService.findByEmail(principal.getName()) : Optional.empty());
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found.");
            model.addAttribute("orders", java.util.Collections.emptyList());
            if (userId != null) {
                model.addAttribute("userId", userId);
            }
            return "user-orders";
        }
        User user = userOpt.get();
        List<Order> orders = orderService.getUserOrders(user);

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("userId", user.getId());

        return "user-orders";
    }

    @GetMapping("/orders/invoice/{orderId}")
    public String viewOrderDetails(@PathVariable Long orderId, Model model, Principal principal,
            @RequestParam(required = false) Long userId) {
        Optional<User> userOpt = (userId != null)
                ? userService.findById(userId)
                : (principal != null ? userService.findByEmail(principal.getName()) : Optional.empty());

        if (userOpt.isEmpty()) {
            return "redirect:/login?error=Please login first";
        }

        User user = userOpt.get();
        Optional<Order> orderOpt = orderService.getOrderById(orderId, user);

        if (orderOpt.isEmpty()) {
            return "redirect:/profile/orders?userId=" + user.getId() + "&error=Order not found";
        }

        Order order = orderOpt.get();
        model.addAttribute("order", order);
        model.addAttribute("orderNumber", order.getOrderNumber() != null ? order.getOrderNumber() : "");
        model.addAttribute("orderDate",
                order.getOrderDate() != null ? order.getOrderDate() : java.time.LocalDateTime.now());
        model.addAttribute("subtotal", order.getTotalAmount() != null ? order.getTotalAmount() : 0.0);
        model.addAttribute("deliveryCharge", order.getDeliveryCharge() != null ? order.getDeliveryCharge() : 0.0);
        model.addAttribute("discount", order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0);
        model.addAttribute("total", order.getFinalAmount() != null ? order.getFinalAmount() : 0.0);
        model.addAttribute("paymentMethod", order.getPaymentMethod() != null ? order.getPaymentMethod() : "");
        model.addAttribute("status",
                order.getStatus() != null ? order.getStatus() : com.example.buildpro.model.Order.OrderStatus.PENDING);

        return "invoice";
    }

    @GetMapping("/edit")
    public String editProfilePage(Model model, Principal principal, @RequestParam(required = false) Long userId) {
        Optional<User> userOpt = (userId != null)
                ? userService.findById(userId)
                : (principal != null ? userService.findByEmail(principal.getName()) : Optional.empty());
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "User not found.");
            // Provide an empty user to avoid Thymeleaf null-bind errors in th:object
            model.addAttribute("user", new User());
            if (userId != null) {
                model.addAttribute("userId", userId);
            }
            return "edit-profile";
        }
        User user = userOpt.get();
        model.addAttribute("user", user);
        model.addAttribute("userId", user.getId());
        return "edit-profile";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute User user, Principal principal,
            @RequestParam(required = false) Long userId) {
        Optional<User> userOpt = (userId != null)
                ? userService.findById(userId)
                : (principal != null ? userService.findByEmail(principal.getName()) : Optional.empty());
        if (userOpt.isEmpty()) {
            Long id = userId != null ? userId : null;
            return id != null
                    ? ("redirect:/profile?userId=" + id + "&error=User not found")
                    : "redirect:/profile?error=User not found";
        }
        User existingUser = userOpt.get();
        if (user.getName() != null)
            existingUser.setName(user.getName());
        userService.updateUser(existingUser);
        return "redirect:/profile?userId=" + existingUser.getId() + "&success=Profile updated successfully";
    }
}
