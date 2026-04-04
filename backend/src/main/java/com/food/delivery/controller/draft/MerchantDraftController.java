package com.food.delivery.controller.draft;

import com.food.delivery.common.response.Result;
import com.food.delivery.common.security.AuthConstants;
import com.food.delivery.dto.cart.AddCartItemDTO;
import com.food.delivery.dto.cart.UpdateCartItemQuantityDTO;
import com.food.delivery.service.draft.MerchantDraftService;
import com.food.delivery.vo.draft.CheckoutResultVO;
import com.food.delivery.vo.draft.DraftMerchantVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant-drafts")
public class MerchantDraftController {

    private final MerchantDraftService merchantDraftService;

    public MerchantDraftController(MerchantDraftService merchantDraftService) {
        this.merchantDraftService = merchantDraftService;
    }

    @PostMapping("/items")
    public Result<Void> addItem(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @Valid @RequestBody AddCartItemDTO dto
    ) {
        merchantDraftService.addItem(userId, dto);
        return Result.success(null);
    }

    @GetMapping("/merchants/{merchantId}")
    public Result<DraftMerchantVO> getDraft(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long merchantId
    ) {
        return Result.success(merchantDraftService.getDraft(userId, merchantId));
    }

    @PostMapping("/merchants/{merchantId}/items/{productId}/increase")
    public Result<Void> increaseItem(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long merchantId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityDTO dto
    ) {
        merchantDraftService.increaseItem(userId, merchantId, productId, dto.getQuantity());
        return Result.success(null);
    }

    @PostMapping("/merchants/{merchantId}/items/{productId}/decrease")
    public Result<Void> decreaseItem(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long merchantId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityDTO dto
    ) {
        merchantDraftService.decreaseItem(userId, merchantId, productId, dto.getQuantity());
        return Result.success(null);
    }

    @DeleteMapping("/merchants/{merchantId}/items/{productId}")
    public Result<Void> removeItem(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long merchantId,
            @PathVariable Long productId
    ) {
        merchantDraftService.removeItem(userId, merchantId, productId);
        return Result.success(null);
    }

    @PostMapping("/merchants/{merchantId}/checkout")
    public Result<CheckoutResultVO> checkout(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long merchantId
    ) {
        return Result.success(merchantDraftService.checkout(userId, merchantId));
    }

    @PostMapping("/merchants/{merchantId}/checkout-pending")
    public Result<CheckoutResultVO> checkoutPending(
            @RequestAttribute(AuthConstants.ATTR_USER_ID) Long userId,
            @PathVariable Long merchantId
    ) {
        return Result.success(merchantDraftService.checkoutPending(userId, merchantId));
    }
}
