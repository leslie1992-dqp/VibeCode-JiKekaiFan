package com.food.delivery.controller.merchant;

import com.food.delivery.common.response.Result;
import com.food.delivery.service.coupon.MerchantSeckillCouponService;
import com.food.delivery.service.merchant.MerchantCatalogService;
import com.food.delivery.service.merchant.MerchantReviewService;
import com.food.delivery.vo.common.PageResult;
import com.food.delivery.vo.coupon.SeckillCouponVO;
import com.food.delivery.vo.merchant.MerchantListItemVO;
import com.food.delivery.vo.merchant.MerchantReviewVO;
import com.food.delivery.vo.product.ProductCategoryVO;
import com.food.delivery.vo.product.ProductListItemVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantCatalogService merchantCatalogService;
    private final MerchantSeckillCouponService merchantSeckillCouponService;
    private final MerchantReviewService merchantReviewService;

    public MerchantController(
            MerchantCatalogService merchantCatalogService,
            MerchantSeckillCouponService merchantSeckillCouponService,
            MerchantReviewService merchantReviewService
    ) {
        this.merchantCatalogService = merchantCatalogService;
        this.merchantSeckillCouponService = merchantSeckillCouponService;
        this.merchantReviewService = merchantReviewService;
    }

    @GetMapping
    public Result<PageResult<MerchantListItemVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false, defaultValue = "recommend") String sort
    ) {
        return Result.success(merchantCatalogService.pageMerchants(keyword, pageNo, pageSize, sort));
    }

    @GetMapping("/{merchantId}/reviews")
    public Result<PageResult<MerchantReviewVO>> reviews(
            @PathVariable Long merchantId,
            @RequestParam(defaultValue = "1") long pageNo,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        return Result.success(merchantReviewService.pageReviews(merchantId, pageNo, pageSize));
    }

    @GetMapping("/{merchantId}/categories")
    public Result<List<ProductCategoryVO>> categories(@PathVariable Long merchantId) {
        return Result.success(merchantCatalogService.listCategories(merchantId));
    }

    @GetMapping("/{merchantId}/products")
    public Result<List<ProductListItemVO>> products(
            @PathVariable Long merchantId,
            @RequestParam(required = false) Long categoryId
    ) {
        return Result.success(merchantCatalogService.listProducts(merchantId, categoryId));
    }

    @GetMapping("/{merchantId}/seckill-coupons")
    public Result<List<SeckillCouponVO>> seckillCoupons(@PathVariable Long merchantId) {
        return Result.success(merchantSeckillCouponService.listSeckillCoupons(merchantId));
    }

    @GetMapping("/{merchantId}")
    public Result<MerchantListItemVO> detail(@PathVariable Long merchantId) {
        return Result.success(merchantCatalogService.getMerchant(merchantId));
    }
}
