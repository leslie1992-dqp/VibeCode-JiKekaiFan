package com.food.delivery.controller.user;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.dto.user.UserAddressSaveDTO;
import com.food.delivery.service.user.UserAddressService;
import com.food.delivery.vo.user.UserAddressVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/addresses")
public class UserAddressController {

    private final UserAddressService userAddressService;

    public UserAddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }

    @GetMapping
    public Result<List<UserAddressVO>> list(@RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId) {
        return Result.success(userAddressService.list(userId));
    }

    @PostMapping
    public Result<Map<String, Long>> create(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @Valid @RequestBody UserAddressSaveDTO dto
    ) {
        Long id = userAddressService.create(userId, dto);
        Map<String, Long> data = new HashMap<>();
        data.put("id", id);
        return Result.success(data);
    }

    @PutMapping("/{id}")
    public Result<Void> update(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable("id") Long addressId,
            @Valid @RequestBody UserAddressSaveDTO dto
    ) {
        userAddressService.update(userId, addressId, dto);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable("id") Long addressId
    ) {
        userAddressService.delete(userId, addressId);
        return Result.success(null);
    }
}
