package com.fetch.backend.user.service;

import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.user.dto.AddressDtos;
import com.fetch.backend.user.entity.Address;
import com.fetch.backend.user.repo.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {
    private final AddressRepository repository;
    private final UserService userService;

    public AddressService(AddressRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<AddressDtos.AddressResponse> listMine() {
        Long userId = userService.currentUser().getId();
        return repository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId).stream().map(AddressDtos.AddressResponse::from).toList();
    }

    @Transactional
    public AddressDtos.AddressResponse create(AddressDtos.AddressRequest req) {
        Long userId = userService.currentUser().getId();
        Address a = new Address();
        a.setUserId(userId);
        apply(a, req);
        boolean makeDefault = Boolean.TRUE.equals(req.isDefault()) || repository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId).isEmpty();
        if (makeDefault) repository.clearDefaultForUser(userId, null);
        a.setDefaultAddress(makeDefault);
        return AddressDtos.AddressResponse.from(repository.save(a));
    }

    @Transactional
    public AddressDtos.AddressResponse update(Long id, AddressDtos.AddressRequest req) {
        Long userId = userService.currentUser().getId();
        Address a = repository.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Address not found"));
        apply(a, req);
        if (Boolean.TRUE.equals(req.isDefault())) {
            repository.clearDefaultForUser(userId, id);
            a.setDefaultAddress(true);
        }
        return AddressDtos.AddressResponse.from(repository.save(a));
    }

    @Transactional
    public void delete(Long id) {
        Long userId = userService.currentUser().getId();
        Address a = repository.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Address not found"));
        boolean wasDefault = a.isDefaultAddress();
        repository.delete(a);
        if (wasDefault) {
            repository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(userId).stream().findFirst().ifPresent(next -> {
                next.setDefaultAddress(true);
                repository.save(next);
            });
        }
    }

    private void apply(Address a, AddressDtos.AddressRequest req) {
        a.setType(req.type() == null || req.type().isBlank() ? "SHIPPING" : req.type().trim().toUpperCase());
        a.setFullName(blankToNull(req.fullName()));
        a.setLine1(req.line1().trim());
        a.setLine2(blankToNull(req.line2()));
        a.setCity(req.city().trim());
        a.setState(req.state().trim());
        a.setPostalCode(req.postalCode().trim());
        a.setCountryCode(req.countryCode() == null || req.countryCode().isBlank() ? "US" : req.countryCode().trim().toUpperCase());
        a.setPhone(blankToNull(req.phone()));
    }

    private String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
