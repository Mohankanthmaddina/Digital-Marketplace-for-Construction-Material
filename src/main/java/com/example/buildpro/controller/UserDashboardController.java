package com.example.buildpro.controller;

import com.example.buildpro.model.Order;
import com.example.buildpro.model.User;
import com.example.buildpro.service.OrderService;
import com.example.buildpro.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserDashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/dashboard")
    public String userDashboard(@RequestParam Long userId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String success,
            Model model) {
        Optional<User> userOpt = userService.findById(userId);

        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        List<Order> userOrders = orderService.getUserOrders(user);

        // Get latest order if orderId is provided
        Order latestOrder = null;
        if (orderId != null) {
            try {
                Optional<Order> orderOpt = orderService.getOrderById(orderId, user);
                if (orderOpt.isPresent()) {
                    latestOrder = orderOpt.get();
                }
            } catch (Exception e) {
                // Order not found, continue without it
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("orders", userOrders);
        model.addAttribute("latestOrder", latestOrder);
        model.addAttribute("successMessage", success);
        model.addAttribute("userId", userId);

        return "user-dashboard";
    }

    @GetMapping("/orders")
    public String userOrders(@RequestParam Long userId, Model model) {
        Optional<User> userOpt = userService.findById(userId);

        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }

        User user = userOpt.get();
        List<Order> userOrders = orderService.getUserOrders(user);

        model.addAttribute("user", user);
        model.addAttribute("orders", userOrders);
        model.addAttribute("userId", userId);

        return "user-orders";
    }
}
