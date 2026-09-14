package com.diyfigure.repository;

import com.diyfigure.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 收货地址 Repository
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * 查询用户的所有收货地址
     */
    List<Address> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    List<Address> findByUserIdAndIsDefaultTrue(Long userId);
}
