package com.ecommerce.user.application;

import com.ecommerce.user.domain.aggregate.User;
import com.ecommerce.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User createNewUser(String email) {
        User user = User.builder().email(email).build();
        userRepository.save(user);
        return user;
    }
}
