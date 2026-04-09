package com.food.delivery.impl.cart;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.common.order.OrderStatus;
import com.food.delivery.entity.cart.CartItemEntity;
import com.food.delivery.entity.coupon.MerchantSeckillCouponEntity;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.entity.order.OrderEntity;
import com.food.delivery.entity.order.OrderItemEntity;
import com.food.delivery.entity.product.ProductEntity;
import com.food.delivery.impl.coupon.DraftCouponSupport;
import com.food.delivery.mapper.cart.CartItemMapper;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.mapper.order.OrderItemMapper;
import com.food.delivery.mapper.order.OrderMapper;
import com.food.delivery.mapper.product.ProductMapper;
import com.food.delivery.service.cart.CartCheckoutService;
import com.food.delivery.service.coupon.UserCouponLifecycleService;
import com.food.delivery.service.delivery.DeliveryDispatchService;
import com.food.delivery.service.order.OrderSalesService;
import com.food.delivery.support.OrderNoAllocator;
import com.food.delivery.vo.cart.CartCheckoutBatchResultVO;
import com.food.delivery.vo.cart.CartCheckoutPreviewVO;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class CartCheckoutServiceImpl implements CartCheckoutService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int PENDING_PAYMENT_MINUTES = 30;

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderNoAllocator orderNoAllocator;
    private final DraftCouponSupport draftCouponSupport;
    private final UserCouponLifecycleService userCouponLifecycleService;
    private final OrderSalesService orderSalesService;
    private final DeliveryDispatchService deliveryDispatchService;

    public CartCheckoutServiceImpl(
            CartItemMapper cartItemMapper,
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
        this.cartItemMapper = cartItemMapper;
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
    public CartCheckoutPreviewVO preview(Long userId, List<Long> productIds) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        List<Long> distinct = distinctProductIds(productIds);
        if (distinct.isEmpty()) {
            throw new BizException(40001, "no product selected");
        }
        Map<Long, List<CartItemEntity>> byMerchant = groupSelectedCartRows(userId, distinct);
        List<DraftMerchantVO> merchants = new ArrayList<>();
        BigDecimal grand = ZERO;
        for (Map.Entry<Long, List<CartItemEntity>> e : byMerchant.entrySet()) {
            DraftMerchantVO vo = buildMerchantPreviewVo(userId, e.getKey(), e.getValue());
            merchants.add(vo);
            grand = grand.add(vo.getPayableAmount() == null ? ZERO : vo.getPayableAmount());
        }
        CartCheckoutPreviewVO out = new CartCheckoutPreviewVO();
        out.setMerchants(merchants);
        out.setGrandPayable(grand);
        return out;
    }

    private DraftMerchantVO buildMerchantPreviewVo(Long userId, Long merchantId, List<CartItemEntity> cartRows) {
        MerchantEntity merchant = merchantMapper.selectById(merchantId);
        List<DraftItemVO> draftItems = new ArrayList<>();
        int totalQty = 0;
        BigDecimal goodsSum = ZERO;
        for (CartItemEntity row : cartRows) {
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
            draftItems.add(di);
        }
        BigDecimal delivery = merchant == null ? ZERO : normalizeDeliveryFee(merchant.getDeliveryFee());
        DraftMerchantVO vo = new DraftMerchantVO();
        vo.setMerchantId(merchantId);
        vo.setMerchantName(merchant == null ? null : merchant.getName());
        vo.setItems(draftItems);
        vo.setTotalQuantity(totalQty);
        vo.setGoodsAmount(goodsSum);
        vo.setDeliveryFee(delivery);
        draftCouponSupport.applyToDraftVo(vo, userId, merchantId, goodsSum, delivery);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartCheckoutBatchResultVO checkout(Long userId, List<Long> productIds, boolean immediate) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        List<Long> distinct = distinctProductIds(productIds);
        if (distinct.isEmpty()) {
            throw new BizException(40001, "no product selected");
        }
        Map<Long, List<CartItemEntity>> byMerchant = groupSelectedCartRows(userId, distinct);
        List<CheckoutResultVO> results = new ArrayList<>();
        for (Map.Entry<Long, List<CartItemEntity>> e : byMerchant.entrySet()) {
            CheckoutResultVO one = placeOrderForMerchant(userId, e.getKey(), e.getValue(), immediate);
            results.add(one);
        }
        CartCheckoutBatchResultVO batch = new CartCheckoutBatchResultVO();
        batch.setOrders(results);
        return batch;
    }

    private CheckoutResultVO placeOrderForMerchant(
            Long userId,
            Long merchantId,
            List<CartItemEntity> cartRows,
            boolean immediate
    ) {
        CartOrderBuild b = buildOrderFromCartRows(merchantId, cartRows);
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
        LocalDateTime expireAt = immediate ? null : b.now.plus(Duration.ofMinutes(PENDING_PAYMENT_MINUTES));

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
        order.setStatus(immediate ? OrderStatus.PAID_SUCCESS : OrderStatus.PENDING_PAYMENT);
        order.setExpireAt(expireAt);
        order.setCreatedAt(b.now);
        order.setUpdatedAt(b.now);
        orderMapper.insert(order);

        if (immediate && userCouponId != null) {
            userCouponLifecycleService.markUsed(userCouponId);
        }
        if (!immediate && userCouponId != null) {
            userCouponLifecycleService.lockForPendingOrder(userCouponId, order.getId());
        }

        persistOrderLinesAndClearCart(order.getId(), b, b.now);
        if (immediate) {
            orderSalesService.applyPaidOrder(order.getId());
            deliveryDispatchService.createAndDispatchForPaidOrder(order.getId());
        }

        CheckoutResultVO r = new CheckoutResultVO();
        r.setOrderId(order.getId());
        r.setOrderNo(orderNo);
        r.setMerchantId(merchantId);
        r.setPayAmount(payAmount);
        return r;
    }

    private void persistOrderLinesAndClearCart(Long orderId, CartOrderBuild b, LocalDateTime now) {
        for (OrderItemEntity oi : b.orderLines) {
            oi.setOrderId(orderId);
            orderItemMapper.insert(oi);
        }
        for (CartItemEntity row : b.cartRows) {
            row.setStatus(0);
            row.setUpdatedAt(now);
            cartItemMapper.updateById(row);
        }
    }

    private CartOrderBuild buildOrderFromCartRows(Long merchantId, List<CartItemEntity> cartRows) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal goodsTotal = ZERO;
        List<OrderItemEntity> orderLines = new ArrayList<>();
        for (CartItemEntity row : cartRows) {
            if (row == null || row.getProductId() == null) {
                continue;
            }
            ProductEntity product = productMapper.selectById(row.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new BizException(40406, "product not found");
            }
            if (product.getMerchantId() == null || !product.getMerchantId().equals(merchantId)) {
                throw new BizException(40001, "product does not belong to merchant");
            }
            int q = row.getQuantity() == null ? 1 : row.getQuantity();
            if (product.getStock() != null && product.getStock() >= 0 && q > product.getStock()) {
                throw new BizException(40007, "stock not enough");
            }
            BigDecimal price = product.getPrice() == null ? ZERO : product.getPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(q)).setScale(2, RoundingMode.HALF_UP);
            goodsTotal = goodsTotal.add(subtotal);
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
        CartOrderBuild b = new CartOrderBuild();
        b.goodsTotal = goodsTotal;
        b.deliveryFee = delivery;
        b.total = goodsTotal.add(delivery);
        b.orderLines = orderLines;
        b.cartRows = cartRows;
        b.now = now;
        return b;
    }

    /**
     * 按商家分组购物车行；校验所选商品均在购物车且有效。
     */
    private static List<Long> distinctProductIds(List<Long> productIds) {
        if (productIds == null) {
            return List.of();
        }
        return productIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private Map<Long, List<CartItemEntity>> groupSelectedCartRows(Long userId, List<Long> productIds) {
        Map<Long, List<CartItemEntity>> byMerchant = new TreeMap<>();
        for (Long productId : productIds) {
            if (productId == null || productId < 1) {
                throw new BizException(40001, "invalid product id");
            }
            CartItemEntity row = cartItemMapper.selectOne(
                    new LambdaQueryWrapper<CartItemEntity>()
                            .eq(CartItemEntity::getUserId, userId)
                            .eq(CartItemEntity::getProductId, productId)
                            .eq(CartItemEntity::getStatus, 1)
            );
            if (row == null) {
                throw new BizException(40008, "cart item not found for product " + productId);
            }
            Long mid = row.getMerchantId();
            if (mid == null) {
                throw new BizException(50000, "cart merchantId missing");
            }
            byMerchant.computeIfAbsent(mid, k -> new ArrayList<>()).add(row);
        }
        return byMerchant;
    }

    private BigDecimal normalizeDeliveryFee(BigDecimal fee) {
        if (fee == null) {
            return ZERO;
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    private static final class CartOrderBuild {
        private BigDecimal goodsTotal;
        private BigDecimal deliveryFee;
        private BigDecimal total;
        private List<OrderItemEntity> orderLines;
        private List<CartItemEntity> cartRows;
        private LocalDateTime now;
    }
}
