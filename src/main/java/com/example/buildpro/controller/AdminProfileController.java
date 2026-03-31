package com.example.buildpro.controller;

import com.example.buildpro.model.User;
import com.example.buildpro.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/admin/profile")
@CrossOrigin(origins = "*")
public class AdminProfileController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String adminProfilePage(Model model, Principal principal, @RequestParam(required = false) Long adminId) {
        Optional<User> adminOpt = (adminId != null)
                ? userService.findById(adminId)
                : (principal != null ? userService.findByEmail(principal.getName()) : Optional.empty());
        
        if (adminOpt.isEmpty()) {
            model.addAttribute("error", "Admin not found.");
            model.addAttribute("admin", new User());
            if (adminId != null) {
                model.addAttribute("adminId", adminId);
            }
            return "admin-profile";
        }
        
        User admin = adminOpt.get();
        
        // Verify this is actually an admin
        if (admin.getRole() != User.Role.ADMIN) {
            model.addAttribute("error", "Access denied. This page is for administrators only.");
            return "admin-profile";
        }
        
        model.addAttribute("admin", admin);
        model.addAttribute("adminId", admin.getId());

        return "admin-profile";
    }

    @GetMapping("/edit")
    public String editAdminProfilePage(Model model, Principal principal, @RequestParam(required = false) Long adminId) {
        Optional<User> adminOpt = (adminId != null)
                ? userService.findById(adminId)
                : (principal != null ? userService.findByEmail(principal.getName()) : Optional.empty());
        
        if (adminOpt.isEmpty()) {
            model.addAttribute("error", "Admin not found.");
            model.addAttribute("admin", new User());
            if (adminId != null) {
                model.addAttribute("adminId", adminId);
            }
            return "admin-edit-profile";
        }
        
        User admin = adminOpt.get();
        
        // Verify this is actually an admin
        if (admin.getRole() != User.Role.ADMIN) {
            model.addAttribute("error", "Access denied. This page is for administrators only.");
            return "admin-edit-profile";
        }
        
        model.addAttribute("admin", admin);
        model.addAttribute("adminId", admin.getId());
        
        return "admin-edit-profile";
    }

    @PostMapping("/update")
    public String updateAdminProfile(@ModelAttribute User admin, Principal principal, @RequestParam(required = false) Long adminId) {
        Optional<User> adminOpt = (adminId != null)
                ? userService.findById(adminId)
                : (principal != null ? userService.findByEmail(principal.getName()) : Optional.empty());
        
        if (adminOpt.isEmpty()) {
            Long id = adminId != null ? adminId : null;
            return id != null
                    ? ("redirect:/admin/profile?adminId=" + id + "&error=Admin not found")
                    : "redirect:/admin/profile?error=Admin not found";
        }
        
        User existingAdmin = adminOpt.get();
        
        // Verify this is actually an admin
        if (existingAdmin.getRole() != User.Role.ADMIN) {
            return "redirect:/admin/profile?error=Access denied";
        }
        
        // Update only allowed fields
        if (admin.getName() != null && !admin.getName().trim().isEmpty()) {
            existingAdmin.setName(admin.getName());
        }
        
        userService.updateUser(existingAdmin);
        
        return "redirect:/admin/profile?adminId=" + existingAdmin.getId() + "&success=Profile updated successfully";
    }
}




