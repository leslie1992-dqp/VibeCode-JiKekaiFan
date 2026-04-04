package com.food.delivery.service.user;

import com.food.delivery.dto.user.UserAddressSaveDTO;
import com.food.delivery.vo.user.UserAddressVO;

import java.util.List;

public interface UserAddressService {
    List<UserAddressVO> list(Long userId);

    Long create(Long userId, UserAddressSaveDTO dto);

    void update(Long userId, Long addressId, UserAddressSaveDTO dto);

    void delete(Long userId, Long addressId);
}
