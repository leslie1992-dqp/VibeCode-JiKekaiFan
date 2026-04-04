package com.food.delivery.impl.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.dto.user.UserAddressSaveDTO;
import com.food.delivery.entity.user.UserAddressEntity;
import com.food.delivery.mapper.user.UserAddressMapper;
import com.food.delivery.service.user.UserAddressService;
import com.food.delivery.vo.user.UserAddressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressMapper userAddressMapper;

    public UserAddressServiceImpl(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    @Override
    public List<UserAddressVO> list(Long userId) {
        List<UserAddressEntity> list = userAddressMapper.selectList(
                new LambdaQueryWrapper<UserAddressEntity>()
                        .eq(UserAddressEntity::getUserId, userId)
                        .orderByDesc(UserAddressEntity::getIsDefault)
                        .orderByDesc(UserAddressEntity::getId)
        );
        return list.stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, UserAddressSaveDTO dto) {
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefault(userId);
        }
        UserAddressEntity entity = toEntity(userId, dto);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userAddressMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long addressId, UserAddressSaveDTO dto) {
        UserAddressEntity existed = requireOwned(userId, addressId);
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            clearDefault(userId);
        }
        existed.setContactName(dto.getContactName());
        existed.setContactPhone(dto.getContactPhone());
        existed.setProvince(dto.getProvince());
        existed.setCity(dto.getCity());
        existed.setDistrict(dto.getDistrict());
        existed.setDetail(dto.getDetail());
        existed.setLatitude(dto.getLatitude());
        existed.setLongitude(dto.getLongitude());
        existed.setIsDefault(dto.getIsDefault());
        existed.setUpdatedAt(LocalDateTime.now());
        userAddressMapper.updateById(existed);
    }

    @Override
    public void delete(Long userId, Long addressId) {
        requireOwned(userId, addressId);
        userAddressMapper.deleteById(addressId);
    }

    private UserAddressEntity requireOwned(Long userId, Long addressId) {
        UserAddressEntity entity = userAddressMapper.selectById(addressId);
        if (entity == null || !userId.equals(entity.getUserId())) {
            throw new BizException(40404, "address not found");
        }
        return entity;
    }

    private void clearDefault(Long userId) {
        userAddressMapper.update(
                null,
                new LambdaUpdateWrapper<UserAddressEntity>()
                        .eq(UserAddressEntity::getUserId, userId)
                        .set(UserAddressEntity::getIsDefault, 0)
        );
    }

    private UserAddressEntity toEntity(Long userId, UserAddressSaveDTO dto) {
        UserAddressEntity entity = new UserAddressEntity();
        entity.setUserId(userId);
        entity.setContactName(dto.getContactName());
        entity.setContactPhone(dto.getContactPhone());
        entity.setProvince(dto.getProvince());
        entity.setCity(dto.getCity());
        entity.setDistrict(dto.getDistrict());
        entity.setDetail(dto.getDetail());
        entity.setLatitude(dto.getLatitude());
        entity.setLongitude(dto.getLongitude());
        entity.setIsDefault(dto.getIsDefault() != null && dto.getIsDefault() == 1 ? 1 : 0);
        return entity;
    }

    private UserAddressVO toVo(UserAddressEntity e) {
        UserAddressVO vo = new UserAddressVO();
        vo.setId(e.getId());
        vo.setContactName(e.getContactName());
        vo.setContactPhone(e.getContactPhone());
        vo.setProvince(e.getProvince());
        vo.setCity(e.getCity());
        vo.setDistrict(e.getDistrict());
        vo.setDetail(e.getDetail());
        vo.setLatitude(e.getLatitude());
        vo.setLongitude(e.getLongitude());
        vo.setIsDefault(e.getIsDefault());
        return vo;
    }
}
