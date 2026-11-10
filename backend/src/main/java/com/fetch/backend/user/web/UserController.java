package com.fetch.backend.user.web;

import com.fetch.backend.user.dto.UserDtos;
import com.fetch.backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDtos.UserResponse me() {
        return userService.me();
    }

    @PatchMapping("/me")
    public UserDtos.UserResponse updateMe(@Valid @RequestBody UserDtos.UpdateMeRequest request) {
        return userService.updateMe(request);
    }
}
