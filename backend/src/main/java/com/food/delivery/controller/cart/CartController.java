package com.food.delivery.controller.cart;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.dto.cart.AddCartItemDTO;
import com.food.delivery.dto.cart.CartCheckoutApplyDTO;
import com.food.delivery.dto.cart.CartCheckoutProductIdsDTO;
import com.food.delivery.dto.cart.UpdateCartItemQuantityDTO;
import com.food.delivery.service.cart.CartCheckoutService;
import com.food.delivery.service.cart.CartService;
import com.food.delivery.vo.cart.CartCheckoutBatchResultVO;
import com.food.delivery.vo.cart.CartCheckoutPreviewVO;
import com.food.delivery.vo.cart.CartItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final CartCheckoutService cartCheckoutService;

    public CartController(CartService cartService, CartCheckoutService cartCheckoutService) {
        this.cartService = cartService;
        this.cartCheckoutService = cartCheckoutService;
    }

    @PostMapping("/items")
    public Result<Void> addItem(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @Valid @RequestBody AddCartItemDTO dto
    ) {
        cartService.addItem(userId, dto);
        return Result.success(null);
    }

    @GetMapping("/items")
    public Result<List<CartItemVO>> listItems(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId
    ) {
        return Result.success(cartService.listItems(userId));
    }

    @PostMapping("/checkout-preview")
    public Result<CartCheckoutPreviewVO> checkoutPreview(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @Valid @RequestBody CartCheckoutProductIdsDTO dto
    ) {
        return Result.success(cartCheckoutService.preview(userId, dto.getProductIds()));
    }

    @PostMapping("/checkout")
    public Result<CartCheckoutBatchResultVO> checkout(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @Valid @RequestBody CartCheckoutApplyDTO dto
    ) {
        return Result.success(cartCheckoutService.checkout(userId, dto.getProductIds(), dto.isImmediate()));
    }

    @DeleteMapping("/clear")
    public Result<Void> clearAll(@RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId) {
        cartService.clearAll(userId);
        return Result.success(null);
    }

    @DeleteMapping("/items/{productId}")
    public Result<Void> removeItem(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long productId
    ) {
        cartService.removeItem(userId, productId);
        return Result.success(null);
    }

    @PostMapping("/items/{productId}/increase")
    public Result<Void> increaseItem(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityDTO dto
    ) {
        cartService.increaseItem(userId, productId, dto.getQuantity());
        return Result.success(null);
    }

    @PostMapping("/items/{productId}/decrease")
    public Result<Void> decreaseItem(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityDTO dto
    ) {
        cartService.decreaseItem(userId, productId, dto.getQuantity());
        return Result.success(null);
    }
}

