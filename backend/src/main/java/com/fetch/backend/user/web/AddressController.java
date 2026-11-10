package com.fetch.backend.user.web;

import com.fetch.backend.user.dto.AddressDtos;
import com.fetch.backend.user.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) { this.addressService = addressService; }

    @GetMapping
    public List<AddressDtos.AddressResponse> list() { return addressService.listMine(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDtos.AddressResponse create(@Valid @RequestBody AddressDtos.AddressRequest request) { return addressService.create(request); }

    @PutMapping("/{id}")
    public AddressDtos.AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressDtos.AddressRequest request) { return addressService.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { addressService.delete(id); }
}
