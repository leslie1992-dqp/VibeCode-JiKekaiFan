package com.food.delivery.impl.draft;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.common.order.OrderStatus;
import com.food.delivery.dto.cart.AddCartItemDTO;
import com.food.delivery.entity.draft.MerchantOrderDraftEntity;
import com.food.delivery.entity.draft.MerchantOrderDraftItemEntity;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.entity.order.OrderItemEntity;
import com.food.delivery.entity.coupon.MerchantSeckillCouponEntity;
import com.food.delivery.entity.product.ProductEntity;
import com.food.delivery.impl.coupon.DraftCouponSupport;
import com.food.delivery.mapper.draft.MerchantOrderDraftItemMapper;
import com.food.delivery.mapper.draft.MerchantOrderDraftMapper;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.mapper.order.OrderItemMapper;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.mapper.product.ProductMapper;
import com.food.delivery.service.coupon.UserCouponLifecycleService;
import com.food.delivery.service.delivery.DeliveryDispatchService;
import com.food.delivery.service.draft.MerchantDraftService;
import com.food.delivery.service.order.OrderSalesService;
import com.food.delivery.support.OrderNoAllocator;
import com.food.delivery.vo.draft.CheckoutResultVO;
import com.food.delivery.vo.draft.DraftItemVO;
import com.food.delivery.vo.draft.DraftMerchantVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MerchantDraftServiceImpl implements MerchantDraftService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int PENDING_PAYMENT_MINUTES = 30;

    private final MerchantOrderDraftMapper draftMapper;
    private final MerchantOrderDraftItemMapper draftItemMapper;
    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderNoAllocator orderNoAllocator;
    private final DraftCouponSupport draftCouponSupport;
    private final UserCouponLifecycleService userCouponLifecycleService;
    private final OrderSalesService orderSalesService;
    private final DeliveryDispatchService deliveryDispatchService;

    public MerchantDraftServiceImpl(
            MerchantOrderDraftMapper draftMapper,
            MerchantOrderDraftItemMapper draftItemMapper,
            ProductMapper productMapper,
            MerchantMapper merchantMapper,
            OrderMapper orderMapper,
            OrderItemMapper orderItemMapper,
            OrderNoAllocator orderNoAllocator,
            DraftCouponSupport draftCouponSupport,
            UserCouponLifecycleService userCouponLifecycleService,
            OrderSalesService orderSalesService,
            DeliveryDispatchService deliveryDispatchService
    ) {
        this.draftMapper = draftMapper;
        this.draftItemMapper = draftItemMapper;
        this.productMapper = productMapper;
        this.merchantMapper = merchantMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderNoAllocator = orderNoAllocator;
        this.draftCouponSupport = draftCouponSupport;
        this.userCouponLifecycleService = userCouponLifecycleService;
        this.orderSalesService = orderSalesService;
        this.deliveryDispatchService = deliveryDispatchService;
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

        Long merchantId = product.getMerchantId();
        MerchantOrderDraftEntity draft = getOrCreateDraft(userId, merchantId);
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<MerchantOrderDraftItemEntity> w = new LambdaQueryWrapper<MerchantOrderDraftItemEntity>()
                .eq(MerchantOrderDraftItemEntity::getDraftId, draft.getId())
                .eq(MerchantOrderDraftItemEntity::getProductId, dto.getProductId());

        MerchantOrderDraftItemEntity existed = draftItemMapper.selectOne(w);
        if (existed == null) {
            if (product.getStock() != null && product.getStock() >= 0 && quantity > product.getStock()) {
                throw new BizException(40007, "stock not enough");
            }
            MerchantOrderDraftItemEntity entity = new MerchantOrderDraftItemEntity();
            entity.setDraftId(draft.getId());
            entity.setMerchantId(merchantId);
            entity.setProductId(dto.getProductId());
            entity.setQuantity(quantity);
            entity.setStatus(1);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            draftItemMapper.insert(entity);
            return;
        }

        boolean wasDeleted = existed.getStatus() != null && existed.getStatus() == 0;
        int existedQty = wasDeleted ? 0 : (existed.getQuantity() == null ? 0 : existed.getQuantity());
        int newQty = existedQty + quantity;
        if (product.getStock() != null && product.getStock() >= 0 && newQty > product.getStock()) {
            throw new BizException(40007, "stock not enough");
        }
        existed.setStatus(1);
        existed.setQuantity(newQty);
        existed.setMerchantId(merchantId);
        existed.setUpdatedAt(now);
        draftItemMapper.updateById(existed);
    }

    @Override
    public DraftMerchantVO getDraft(Long userId, Long merchantId) {
        DraftMerchantVO vo = new DraftMerchantVO();
        vo.setMerchantId(merchantId);

        if (userId == null || userId < 1 || merchantId == null || merchantId < 1) {
            vo.setItems(Collections.emptyList());
            vo.setTotalQuantity(0);
            vo.setGoodsAmount(ZERO);
            vo.setDeliveryFee(ZERO);
            draftCouponSupport.applyToDraftVo(vo, userId, merchantId, ZERO, ZERO);
            return vo;
        }

        MerchantOrderDraftEntity draft = draftMapper.selectOne(
                new LambdaQueryWrapper<MerchantOrderDraftEntity>()
                        .eq(MerchantOrderDraftEntity::getUserId, userId)
                        .eq(MerchantOrderDraftEntity::getMerchantId, merchantId)
                        .eq(MerchantOrderDraftEntity::getStatus, 1)
        );
        if (draft == null) {
            vo.setItems(Collections.emptyList());
            vo.setTotalQuantity(0);
            vo.setGoodsAmount(ZERO);
            vo.setDeliveryFee(ZERO);
            draftCouponSupport.applyToDraftVo(vo, userId, merchantId, ZERO, ZERO);
            return vo;
        }

        List<MerchantOrderDraftItemEntity> rows = draftItemMapper.selectList(
                new LambdaQueryWrapper<MerchantOrderDraftItemEntity>()
                        .eq(MerchantOrderDraftItemEntity::getDraftId, draft.getId())
                        .eq(MerchantOrderDraftItemEntity::getStatus, 1)
        );
        if (rows == null || rows.isEmpty()) {
            vo.setItems(Collections.emptyList());
            vo.setTotalQuantity(0);
            vo.setGoodsAmount(ZERO);
            vo.setDeliveryFee(ZERO);
            draftCouponSupport.applyToDraftVo(vo, userId, merchantId, ZERO, ZERO);
            return vo;
        }

        List<DraftItemVO> items = new ArrayList<>();
        int totalQty = 0;
        BigDecimal goodsSum = ZERO;
        for (MerchantOrderDraftItemEntity row : rows) {
            if (row == null || row.getProductId() == null) {
                continue;
            }
            ProductEntity product = productMapper.selectById(row.getProductId());
            if (product == null) {
                continue;
            }
            int q = row.getQuantity() == null ? 1 : row.getQuantity();
            totalQty += q;
            BigDecimal price = product.getPrice() == null ? ZERO : product.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(q)).setScale(2, RoundingMode.HALF_UP);
            goodsSum = goodsSum.add(subtotal);

            DraftItemVO di = new DraftItemVO();
            di.setMerchantId(merchantId);
            di.setProductId(row.getProductId());
            di.setProductName(product.getName());
            di.setPrice(price);
            di.setQuantity(q);
            di.setSubtotal(subtotal);
            items.add(di);
        }

        MerchantEntity merchant = merchantMapper.selectById(merchantId);
        BigDecimal delivery = merchant == null ? ZERO : normalizeDeliveryFee(merchant.getDeliveryFee());

        vo.setItems(items);
        vo.setTotalQuantity(totalQty);
        vo.setGoodsAmount(goodsSum);
        vo.setDeliveryFee(delivery);
        draftCouponSupport.applyToDraftVo(vo, userId, merchantId, goodsSum, delivery);
        return vo;
    }

    private BigDecimal normalizeDeliveryFee(BigDecimal fee) {
        if (fee == null) {
            return ZERO;
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseItem(Long userId, Long merchantId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BizException(40001, "invalid quantity");
        }
        AddCartItemDTO dto = new AddCartItemDTO();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        validateMerchantProduct(merchantId, productId);
        addItem(userId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decreaseItem(Long userId, Long merchantId, Long productId, Integer quantity) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (merchantId == null || merchantId < 1 || productId == null || productId < 1) {
            throw new BizException(40001, "invalid request");
        }
        if (quantity == null || quantity < 1) {
            throw new BizException(40001, "invalid quantity");
        }

        MerchantOrderDraftEntity draft = requireDraft(userId, merchantId);
        MerchantOrderDraftItemEntity existed = draftItemMapper.selectOne(
                new LambdaQueryWrapper<MerchantOrderDraftItemEntity>()
                        .eq(MerchantOrderDraftItemEntity::getDraftId, draft.getId())
                        .eq(MerchantOrderDraftItemEntity::getProductId, productId)
                        .eq(MerchantOrderDraftItemEntity::getStatus, 1)
        );
        if (existed == null) {
            throw new BizException(40406, "draft item not found");
        }

        int oldQty = existed.getQuantity() == null ? 0 : existed.getQuantity();
        int newQty = oldQty - quantity;
        LocalDateTime now = LocalDateTime.now();
        if (newQty <= 0) {
            existed.setStatus(0);
        } else {
            existed.setQuantity(newQty);
        }
        existed.setUpdatedAt(now);
        draftItemMapper.updateById(existed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long userId, Long merchantId, Long productId) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (merchantId == null || merchantId < 1 || productId == null || productId < 1) {
            throw new BizException(40001, "invalid request");
        }

        MerchantOrderDraftEntity draft = requireDraft(userId, merchantId);
        MerchantOrderDraftItemEntity existed = draftItemMapper.selectOne(
                new LambdaQueryWrapper<MerchantOrderDraftItemEntity>()
                        .eq(MerchantOrderDraftItemEntity::getDraftId, draft.getId())
                        .eq(MerchantOrderDraftItemEntity::getProductId, productId)
                        .eq(MerchantOrderDraftItemEntity::getStatus, 1)
        );
        if (existed == null) {
            throw new BizException(40406, "draft item not found");
        }
        existed.setStatus(0);
        existed.setUpdatedAt(LocalDateTime.now());
        draftItemMapper.updateById(existed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckoutResultVO checkout(Long userId, Long merchantId) {
        DraftOrderBuild b = buildOrderFromDraft(userId, merchantId);
        DraftCouponSupport.CouponApplyResult cap = draftCouponSupport.resolveForCheckout(
                userId, merchantId, b.goodsTotal, b.total
        );
        BigDecimal couponAmount = ZERO;
        Long userCouponId = null;
        String couponCode = null;
        if (cap != null) {
            couponAmount = cap.getDiscountAmount();
            userCouponId = cap.getUserCouponId();
            MerchantSeckillCouponEntity tpl = cap.getTemplate();
            if (tpl != null && tpl.getId() != null) {
                couponCode = "SECKILL-" + tpl.getId();
            }
        }
        BigDecimal payAmount = b.total.subtract(couponAmount).max(ZERO);

        String orderNo = orderNoAllocator.allocate();
        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setGoodsAmount(b.goodsTotal);
        order.setDeliveryFee(b.deliveryFee);
        order.setTotalAmount(b.total);
        order.setCouponCode(couponCode);
        order.setUserCouponId(userCouponId);
        order.setCouponAmount(couponAmount);
        order.setPayAmount(payAmount);
        order.setStatus(OrderStatus.PAID_SUCCESS);
        order.setExpireAt(null);
        order.setCreatedAt(b.now);
        order.setUpdatedAt(b.now);
        orderMapper.insert(order);

        if (userCouponId != null) {
            userCouponLifecycleService.markUsed(userCouponId);
        }

        persistOrderLinesAndClearDraft(order.getId(), b, b.now);
        orderSalesService.applyPaidOrder(order.getId());
        deliveryDispatchService.createAndDispatchForPaidOrder(order.getId());
        return checkoutResult(merchantId, order.getId(), orderNo, payAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckoutResultVO checkoutPending(Long userId, Long merchantId) {
        DraftOrderBuild b = buildOrderFromDraft(userId, merchantId);
        DraftCouponSupport.CouponApplyResult cap = draftCouponSupport.resolveForCheckout(
                userId, merchantId, b.goodsTotal, b.total
        );
        BigDecimal couponAmount = ZERO;
        Long userCouponId = null;
        String couponCode = null;
        if (cap != null) {
            couponAmount = cap.getDiscountAmount();
            userCouponId = cap.getUserCouponId();
            MerchantSeckillCouponEntity tpl = cap.getTemplate();
            if (tpl != null && tpl.getId() != null) {
                couponCode = "SECKILL-" + tpl.getId();
            }
        }
        BigDecimal payAmount = b.total.subtract(couponAmount).max(ZERO);
        LocalDateTime expireAt = b.now.plus(Duration.ofMinutes(PENDING_PAYMENT_MINUTES));

        String orderNo = orderNoAllocator.allocate();
        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setGoodsAmount(b.goodsTotal);
        order.setDeliveryFee(b.deliveryFee);
        order.setTotalAmount(b.total);
        order.setCouponCode(couponCode);
        order.setUserCouponId(userCouponId);
        order.setCouponAmount(couponAmount);
        order.setPayAmount(payAmount);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setExpireAt(expireAt);
        order.setCreatedAt(b.now);
        order.setUpdatedAt(b.now);
        orderMapper.insert(order);

        if (userCouponId != null) {
            userCouponLifecycleService.lockForPendingOrder(userCouponId, order.getId());
        }

        persistOrderLinesAndClearDraft(order.getId(), b, b.now);
        return checkoutResult(merchantId, order.getId(), orderNo, payAmount);
    }

    private CheckoutResultVO checkoutResult(Long merchantId, Long orderId, String orderNo, BigDecimal payAmount) {
        CheckoutResultVO result = new CheckoutResultVO();
        result.setOrderId(orderId);
        result.setOrderNo(orderNo);
        result.setMerchantId(merchantId);
        result.setPayAmount(payAmount);
        return result;
    }

    private void persistOrderLinesAndClearDraft(Long orderId, DraftOrderBuild b, LocalDateTime now) {
        for (OrderItemEntity oi : b.orderLines) {
            oi.setOrderId(orderId);
            orderItemMapper.insert(oi);
        }
        for (MerchantOrderDraftItemEntity row : b.draftRows) {
            row.setStatus(0);
            row.setUpdatedAt(now);
            draftItemMapper.updateById(row);
        }
    }

    private DraftOrderBuild buildOrderFromDraft(Long userId, Long merchantId) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        if (merchantId == null || merchantId < 1) {
            throw new BizException(40001, "invalid request");
        }

        MerchantOrderDraftEntity draft = draftMapper.selectOne(
                new LambdaQueryWrapper<MerchantOrderDraftEntity>()
                        .eq(MerchantOrderDraftEntity::getUserId, userId)
                        .eq(MerchantOrderDraftEntity::getMerchantId, merchantId)
                        .eq(MerchantOrderDraftEntity::getStatus, 1)
        );
        if (draft == null) {
            throw new BizException(40008, "draft is empty");
        }

        List<MerchantOrderDraftItemEntity> rows = draftItemMapper.selectList(
                new LambdaQueryWrapper<MerchantOrderDraftItemEntity>()
                        .eq(MerchantOrderDraftItemEntity::getDraftId, draft.getId())
                        .eq(MerchantOrderDraftItemEntity::getStatus, 1)
        );
        if (rows == null || rows.isEmpty()) {
            throw new BizException(40008, "draft is empty");
        }

        LocalDateTime now = LocalDateTime.now();
        BigDecimal total = ZERO;
        List<OrderItemEntity> orderLines = new ArrayList<>();

        for (MerchantOrderDraftItemEntity row : rows) {
            ProductEntity product = productMapper.selectById(row.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new BizException(40406, "product not found");
            }
            int q = row.getQuantity() == null ? 1 : row.getQuantity();
            BigDecimal price = product.getPrice() == null ? ZERO : product.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(q)).setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);

            OrderItemEntity oi = new OrderItemEntity();
            oi.setProductId(product.getId());
            oi.setProductName(product.getName());
            oi.setQuantity(q);
            oi.setUnitPrice(price);
            oi.setSubtotal(subtotal);
            oi.setCreatedAt(now);
            oi.setUpdatedAt(now);
            orderLines.add(oi);
        }

        MerchantEntity merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(40404, "merchant not found");
        }
        BigDecimal delivery = normalizeDeliveryFee(merchant.getDeliveryFee());

        DraftOrderBuild b = new DraftOrderBuild();
        b.goodsTotal = total;
        b.deliveryFee = delivery;
        b.total = total.add(delivery);
        b.orderLines = orderLines;
        b.draftRows = rows;
        b.now = now;
        return b;
    }

    private static final class DraftOrderBuild {
        private BigDecimal goodsTotal;
        private BigDecimal deliveryFee;
        private BigDecimal total;
        private List<OrderItemEntity> orderLines;
        private List<MerchantOrderDraftItemEntity> draftRows;
        private LocalDateTime now;
    }

    private MerchantOrderDraftEntity getOrCreateDraft(Long userId, Long merchantId) {
        LocalDateTime now = LocalDateTime.now();
        MerchantOrderDraftEntity draft = draftMapper.selectOne(
                new LambdaQueryWrapper<MerchantOrderDraftEntity>()
                        .eq(MerchantOrderDraftEntity::getUserId, userId)
                        .eq(MerchantOrderDraftEntity::getMerchantId, merchantId)
                        .eq(MerchantOrderDraftEntity::getStatus, 1)
        );
        if (draft != null) {
            return draft;
        }
        MerchantOrderDraftEntity entity = new MerchantOrderDraftEntity();
        entity.setUserId(userId);
        entity.setMerchantId(merchantId);
        entity.setStatus(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        draftMapper.insert(entity);
        return entity;
    }

    private MerchantOrderDraftEntity requireDraft(Long userId, Long merchantId) {
        MerchantOrderDraftEntity draft = draftMapper.selectOne(
                new LambdaQueryWrapper<MerchantOrderDraftEntity>()
                        .eq(MerchantOrderDraftEntity::getUserId, userId)
                        .eq(MerchantOrderDraftEntity::getMerchantId, merchantId)
                        .eq(MerchantOrderDraftEntity::getStatus, 1)
        );
        if (draft == null) {
            throw new BizException(40406, "draft not found");
        }
        return draft;
    }

    private void validateMerchantProduct(Long merchantId, Long productId) {
        ProductEntity product = productMapper.selectById(productId);
        if (product == null || product.getMerchantId() == null || !product.getMerchantId().equals(merchantId)) {
            throw new BizException(40001, "product does not belong to merchant");
        }
    }
}
