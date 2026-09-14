package com.diyfigure.address;

import com.diyfigure.address.dto.AddressRequest;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.Address;
import com.diyfigure.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务
 * 负责用户收货地址的增删改查
 *
 * 抽奖完成后、支付定金前强制填写收货地址(02 文档第 3.6 节)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final com.diyfigure.repository.OrderRepository orderRepository;

    /**
     * 查询用户的所有收货地址
     */
    public List<Address> listByUserId(Long userId) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 查询单个地址详情
     * 校验地址归属权(只能查看自己的地址)
     */
    public Address getByIdAndUserId(Long id, Long userId) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "地址不存在"));
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作此地址");
        }
        return address;
    }

    /**
     * 新增收货地址
     */
    @Transactional
    public Address create(Long userId, AddressRequest request) {
        boolean first = addressRepository.countByUserId(userId) == 0;
        boolean makeDefault = first || Boolean.TRUE.equals(request.getIsDefault());
        if (makeDefault) {
            clearDefault(userId);
        }
        Address address = Address.builder()
                .userId(userId)
                .receiverName(request.getReceiverName())
                .phone(request.getPhone())
                .detail(request.getDetail())
                .isDefault(makeDefault)
                .build();
        address = addressRepository.save(address);
        log.info("新增收货地址: id={}, userId={}", address.getId(), userId);
        return address;
    }

    /**
     * 修改收货地址
     */
    @Transactional
    public Address update(Long id, Long userId, AddressRequest request) {
        Address address = getByIdAndUserId(id, userId);
        address.setReceiverName(request.getReceiverName());
        address.setPhone(request.getPhone());
        address.setDetail(request.getDetail());
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(userId);
            address.setIsDefault(true);
        }
        return addressRepository.save(address);
    }

    /**
     * 删除收货地址
     */
    @Transactional
    public void delete(Long id, Long userId) {
        Address address = getByIdAndUserId(id, userId);
        if (orderRepository.existsByAddressId(id)) {
            throw new BusinessException(ResultCode.CONFLICT, "该地址已被订单使用,不能删除");
        }
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        if (wasDefault) {
            addressRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().findFirst()
                    .ifPresent(next -> {
                        next.setIsDefault(true);
                        addressRepository.save(next);
                    });
        }
        log.info("删除收货地址: id={}, userId={}", id, userId);
    }

    private void clearDefault(Long userId) {
        for (Address existing : addressRepository.findByUserIdAndIsDefaultTrue(userId)) {
            existing.setIsDefault(false);
            addressRepository.save(existing);
        }
    }
}
