package com.diyfigure.address;

import com.diyfigure.address.dto.AddressRequest;
import com.diyfigure.auth.JwtInterceptor;
import com.diyfigure.common.response.ApiResponse;
import com.diyfigure.entity.Address;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址控制器
 *
 * 接口:
 * - GET    /api/addresses          查询当前用户的所有地址
 * - GET    /api/addresses/{id}     查询单个地址详情
 * - POST   /api/addresses          新增地址
 * - PUT    /api/addresses/{id}     修改地址
 * - DELETE /api/addresses/{id}     删除地址
 */
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 查询当前用户的所有收货地址
     */
    @GetMapping
    public ApiResponse<List<Address>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(addressService.listByUserId(userId));
    }

    /**
     * 查询单个地址详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Address> getById(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(addressService.getByIdAndUserId(id, userId));
    }

    /**
     * 新增收货地址
     */
    @PostMapping
    public ApiResponse<Address> create(@Valid @RequestBody AddressRequest body,
                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(addressService.create(userId, body));
    }

    /**
     * 修改收货地址
     */
    @PutMapping("/{id}")
    public ApiResponse<Address> update(@PathVariable Long id,
                                       @Valid @RequestBody AddressRequest body,
                                       HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        return ApiResponse.success(addressService.update(id, userId, body));
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.CURRENT_USER_ID);
        addressService.delete(id, userId);
        return ApiResponse.success();
    }
}
