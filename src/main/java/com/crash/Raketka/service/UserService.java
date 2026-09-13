package com.crash.Raketka.service;

import com.crash.Raketka.domain.User;
import com.crash.Raketka.dto.UserResponse;
import com.crash.Raketka.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("user not found"));
        return new UserResponse(user.getId(), user.getUsername(),
                user.getBalance(), user.getPoints());
    }
}