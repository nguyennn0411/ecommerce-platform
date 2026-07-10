package com.ecommerce.user.api;

import com.ecommerce.user.application.dto.AddressDTO;
import com.ecommerce.user.application.dto.CreateAddressRequest;
import com.ecommerce.user.application.service.AddressService;
import com.ecommerce.user.domain.model.aggregate.Address;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping("/create")
    public ResponseEntity<Void> create(@RequestBody CreateAddressRequest request) {
        addressService.createAddress(
                request.getUserId(),
                request.getReceiverName(),
                request.getReceiverPhone(),
                request.getAddressLine(),
                request.isDefault()
        );
        return ResponseEntity.ok(null);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ArrayList<AddressDTO>> readByUserId(@PathVariable("userId") String userId) {
        ArrayList<Address> addresses = addressService.readByUserId(userId);
        ArrayList<AddressDTO> addressDTOS = new ArrayList<>();
        for (Address address : addresses) {
            AddressDTO addressDTO = new AddressDTO(
                    address.getId(),
                    address.getReceiverName(),
                    address.getReceiverPhone(),
                    address.getGeographicAddress().getAddressLine(),
                    address.isDefault()
            );
            addressDTOS.add(addressDTO);
        }
        return ResponseEntity.ok(addressDTOS);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") int id) {
        addressService.deleteById(id);
        return ResponseEntity.ok(null);
    }
}
