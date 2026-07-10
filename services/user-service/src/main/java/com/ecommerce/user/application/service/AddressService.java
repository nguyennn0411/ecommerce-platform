package com.ecommerce.user.application.service;

import com.ecommerce.user.domain.model.aggregate.Address;
import com.ecommerce.user.domain.model.valueobjects.GeographicAddress;
import com.ecommerce.user.domain.repository.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AddressService {
    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    public void createAddress(String userId,
                     String receiverName,
                     String receiverPhone,
                     String addressLine,
                     boolean isDefault) {
        GeographicAddress geographicAddress = new GeographicAddress(addressLine);
        Address address = new Address(
                userId,
                receiverName,
                receiverPhone,
                geographicAddress,
                isDefault
        );
        addressRepository.save(address);
    }

    public ArrayList<Address> readByUserId(String userId) {
        return addressRepository.findByUserId(userId);
    }

    public void deleteById(int id) {
        addressRepository.deleteById(id);
    }
}
