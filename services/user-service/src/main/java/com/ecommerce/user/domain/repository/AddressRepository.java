package com.ecommerce.user.domain.repository;

import com.ecommerce.user.domain.model.aggregate.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {
    ArrayList<Address> findByUserId(String userId);
}
