package com.fetch.backend.user.dto;

import com.fetch.backend.user.entity.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddressDtos {
    public record AddressRequest(
            @Size(max = 20) String type,
            @Size(max = 150) String fullName,
            @NotBlank @Size(max = 255) String line1,
            @Size(max = 255) String line2,
            @NotBlank @Size(max = 120) String city,
            @NotBlank @Size(max = 120) String state,
            @NotBlank @Size(max = 30) String postalCode,
            @Size(min = 2, max = 2) String countryCode,
            @Size(max = 30) String phone,
            Boolean isDefault
    ) {}

    public record AddressResponse(
            Long id, String type, String fullName, String line1, String line2, String city, String state,
            String postalCode, String countryCode, String phone, boolean isDefault
    ) {
        public static AddressResponse from(Address a) {
            return new AddressResponse(a.getId(), a.getType(), a.getFullName(), a.getLine1(), a.getLine2(),
                    a.getCity(), a.getState(), a.getPostalCode(), a.getCountryCode(), a.getPhone(), a.isDefaultAddress());
        }
    }
}
