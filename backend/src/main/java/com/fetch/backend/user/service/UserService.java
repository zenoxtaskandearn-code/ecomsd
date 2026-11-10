package com.fetch.backend.user.service;

import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.common.error.UnauthorizedException;
import com.fetch.backend.security.UserPrincipal;
import com.fetch.backend.user.dto.UserDtos;
import com.fetch.backend.user.entity.User;
import com.fetch.backend.user.repo.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDtos.UserResponse me() {
        return UserDtos.UserResponse.from(currentUser());
    }

    @Transactional
    public UserDtos.UserResponse updateMe(UserDtos.UpdateMeRequest request) {
        User user = currentUser();
        if (request.firstName() != null) user.setFirstName(request.firstName().trim());
        if (request.lastName() != null) user.setLastName(request.lastName().trim());
        if (request.phone() != null) user.setPhone(request.phone().trim());
        if (request.email() != null && !request.email().trim().equalsIgnoreCase(user.getEmail())) {
            // Email change flow (verification) can be added later; keep simple for MVP.
            user.setEmail(request.email().trim().toLowerCase());
        }
        return UserDtos.UserResponse.from(userRepository.save(user));
    }

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
