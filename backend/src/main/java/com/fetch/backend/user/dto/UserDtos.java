package com.fetch.backend.user.dto;

import com.fetch.backend.user.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UserDtos {
    public record UserResponse(Long id, String email, String firstName, String lastName, String phone, String role, String status) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                    user.getPhone(), user.getRole(), user.getStatus());
        }
    }

    public record UpdateMeRequest(
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName,
            @Size(max = 30) String phone,
            @Email @Size(max = 255) String email
    ) {}
}
