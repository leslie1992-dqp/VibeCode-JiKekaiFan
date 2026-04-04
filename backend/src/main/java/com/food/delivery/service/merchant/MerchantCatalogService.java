package com.food.delivery.service.merchant;

import com.food.delivery.vo.product.ProductCategoryVO;
import com.food.delivery.vo.product.ProductListItemVO;
import com.food.delivery.vo.common.PageResult;
import com.food.delivery.vo.merchant.MerchantListItemVO;

import java.util.List;

public interface MerchantCatalogService {
    PageResult<MerchantListItemVO> pageMerchants(String keyword, long pageNo, long pageSize, String sort);

    MerchantListItemVO getMerchant(Long merchantId);

    List<ProductCategoryVO> listCategories(Long merchantId);

    List<ProductListItemVO> listProducts(Long merchantId, Long categoryId);
}
