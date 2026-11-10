package com.fetch.backend.user.repo;

import com.fetch.backend.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("update Address a set a.defaultAddress = false where a.userId = :userId and (:excludeId is null or a.id <> :excludeId)")
    void clearDefaultForUser(Long userId, Long excludeId);
}
