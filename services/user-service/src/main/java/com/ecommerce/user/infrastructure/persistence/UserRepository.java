package com.ecommerce.user.infrastructure.persistence;

import com.ecommerce.user.domain.aggregate.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
}