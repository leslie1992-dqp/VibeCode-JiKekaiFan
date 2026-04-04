package com.food.delivery.impl.cart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.dto.cart.AddCartItemDTO;
import com.food.delivery.entity.cart.CartItemEntity;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.entity.product.ProductEntity;
import com.food.delivery.mapper.cart.CartItemMapper;
import com.food.delivery.mapper.product.ProductMapper;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.service.cart.CartService;
import com.food.delivery.vo.cart.CartItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class CartServiceImpl implements CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;

    public CartServiceImpl(
            CartItemMapper cartItemMapper,
            ProductMapper productMapper,
            MerchantMapper merchantMapper
    ) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.merchantMapper = merchantMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(Long userId, AddCartItemDTO dto) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (dto == null || dto.getProductId() == null) {
            throw new BizException(40001, "invalid request");
        }
        int quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        if (quantity < 1) {
            quantity = 1;
        }

        ProductEntity product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BizException(40406, "product not found");
        }
        if (product.getMerchantId() == null) {
            throw new BizException(50000, "product merchantId missing");
        }

        LocalDateTime now = LocalDateTime.now();
        // 注意：cart_item 表对 (user_id, product_id) 做了唯一约束。
        // 删除时我们会把 status 设为 0，因此再次“加入”时需要复活这条记录，
        // 而不是尝试插入新行（否则会触发唯一键冲突）。
        LambdaQueryWrapper<CartItemEntity> w = new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getUserId, userId)
                .eq(CartItemEntity::getProductId, dto.getProductId());

        CartItemEntity existed = cartItemMapper.selectOne(w);
        if (existed == null) {
            CartItemEntity entity = new CartItemEntity();
            entity.setUserId(userId);
            entity.setProductId(dto.getProductId());
            entity.setMerchantId(product.getMerchantId());
            entity.setQuantity(quantity);
            entity.setStatus(1);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            cartItemMapper.insert(entity);
            return;
        }

        Integer oldQty = existed.getQuantity();
        // 如果是“已删除（status=0）”的记录，再加入时应从头计数，而不是继续累加旧数量。
        boolean wasDeleted = existed.getStatus() != null && existed.getStatus() == 0;
        int existedQty = wasDeleted ? 0 : (oldQty == null ? 0 : oldQty);
        int newQty = existedQty + quantity;

        if (product.getStock() != null && product.getStock() >= 0 && newQty > product.getStock()) {
            throw new BizException(40007, "stock not enough");
        }

        existed.setStatus(1); // 复活被删除的记录
        existed.setQuantity(newQty);
        existed.setMerchantId(product.getMerchantId());
        existed.setUpdatedAt(now);
        cartItemMapper.updateById(existed);
    }

    @Override
    public void increaseItem(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BizException(40001, "invalid quantity");
        }
        AddCartItemDTO dto = new AddCartItemDTO();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        addItem(userId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseItem(Long userId, Long productId, Integer quantity) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (productId == null || productId < 1) {
            throw new BizException(40001, "invalid request");
        }
        if (quantity == null || quantity < 1) {
            throw new BizException(40001, "invalid quantity");
        }

        LocalDateTime now = LocalDateTime.now();
        CartItemEntity existed = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItemEntity>()
                        .eq(CartItemEntity::getUserId, userId)
                        .eq(CartItemEntity::getProductId, productId)
                        .eq(CartItemEntity::getStatus, 1)
        );
        if (existed == null) {
            throw new BizException(40406, "cart item not found");
        }

        int oldQty = existed.getQuantity() == null ? 0 : existed.getQuantity();
        int newQty = oldQty - quantity;
        if (newQty <= 0) {
            existed.setStatus(0);
        } else {
            existed.setQuantity(newQty);
        }
        existed.setUpdatedAt(now);
        cartItemMapper.updateById(existed);
    }

    @Override
    public List<CartItemVO> listItems(Long userId) {
        if (userId == null || userId < 1) {
            return Collections.emptyList();
        }

        List<CartItemEntity> cartItems = cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItemEntity>()
                        .eq(CartItemEntity::getUserId, userId)
                        .eq(CartItemEntity::getStatus, 1)
        );
        if (cartItems == null || cartItems.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> productIds = cartItems.stream()
                .map(CartItemEntity::getProductId)
                .filter(x -> x != null)
                .collect(Collectors.toSet());
        Set<Long> merchantIds = cartItems.stream()
                .map(CartItemEntity::getMerchantId)
                .filter(x -> x != null)
                .collect(Collectors.toSet());

        Map<Long, ProductEntity> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(ProductEntity::getId, x -> x));
        }

        Map<Long, MerchantEntity> merchantMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            merchantMap = merchantMapper.selectBatchIds(merchantIds).stream()
                    .collect(Collectors.toMap(MerchantEntity::getId, x -> x));
        }

        List<CartItemVO> result = new ArrayList<>();
        for (CartItemEntity item : cartItems) {
            if (item == null || item.getProductId() == null) continue;
            ProductEntity product = productMap.get(item.getProductId());
            if (product == null) continue;

            MerchantEntity merchant = null;
            if (item.getMerchantId() != null) {
                merchant = merchantMap.get(item.getMerchantId());
            }
            CartItemVO vo = new CartItemVO();
            vo.setMerchantId(item.getMerchantId());
            vo.setMerchantName(merchant == null ? null : merchant.getName());
            vo.setProductId(item.getProductId());
            vo.setProductName(product.getName());
            vo.setPrice(product.getPrice());
            vo.setQuantity(item.getQuantity());
            int qty = item.getQuantity() == null ? 1 : item.getQuantity();
            vo.setSubtotal(product.getPrice().multiply(java.math.BigDecimal.valueOf(qty)));
            result.add(vo);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long userId, Long productId) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (productId == null || productId < 1) {
            throw new BizException(40001, "invalid request");
        }

        LocalDateTime now = LocalDateTime.now();
        CartItemEntity existed = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItemEntity>()
                        .eq(CartItemEntity::getUserId, userId)
                        .eq(CartItemEntity::getProductId, productId)
                        .eq(CartItemEntity::getStatus, 1)
        );

        if (existed == null) {
            throw new BizException(40406, "cart item not found");
        }

        existed.setStatus(0);
        existed.setUpdatedAt(now);
        cartItemMapper.updateById(existed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAll(Long userId) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        LocalDateTime now = LocalDateTime.now();
        cartItemMapper.update(
                null,
                new LambdaUpdateWrapper<CartItemEntity>()
                        .eq(CartItemEntity::getUserId, userId)
                        .eq(CartItemEntity::getStatus, 1)
                        .set(CartItemEntity::getStatus, 0)
                        .set(CartItemEntity::getUpdatedAt, now)
        );
    }
}

