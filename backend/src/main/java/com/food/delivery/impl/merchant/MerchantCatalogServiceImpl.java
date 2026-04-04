package com.food.delivery.impl.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.entity.product.ProductCategoryEntity;
import com.food.delivery.entity.product.ProductEntity;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.mapper.product.ProductCategoryMapper;
import com.food.delivery.mapper.product.ProductMapper;
import com.food.delivery.service.merchant.MerchantCatalogService;
import com.food.delivery.vo.common.PageResult;
import com.food.delivery.vo.merchant.MerchantListItemVO;
import com.food.delivery.vo.product.ProductCategoryVO;
import com.food.delivery.vo.product.ProductListItemVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MerchantCatalogServiceImpl implements MerchantCatalogService {

    private final MerchantMapper merchantMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;

    public MerchantCatalogServiceImpl(
            MerchantMapper merchantMapper,
            ProductCategoryMapper productCategoryMapper,
            ProductMapper productMapper
    ) {
        this.merchantMapper = merchantMapper;
        this.productCategoryMapper = productCategoryMapper;
        this.productMapper = productMapper;
    }

    /**
     * sort: recommend（推荐）| distance（距离近→远）| rating（评分高→低）| sales（月售高→低）
     */
    private void applyMerchantSort(LambdaQueryWrapper<MerchantEntity> w, String sort) {
        String s = sort == null ? "" : sort.trim().toLowerCase();
        switch (s) {
            case "distance":
                w.orderByAsc(MerchantEntity::getDistanceKm).orderByAsc(MerchantEntity::getId);
                break;
            case "rating":
                w.orderByDesc(MerchantEntity::getRating).orderByAsc(MerchantEntity::getId);
                break;
            case "sales":
                w.orderByDesc(MerchantEntity::getMonthlySales).orderByAsc(MerchantEntity::getId);
                break;
            case "recommend":
            default:
                w.orderByDesc(MerchantEntity::getRecommendScore)
                        .orderByDesc(MerchantEntity::getRating)
                        .orderByAsc(MerchantEntity::getId);
                break;
        }
    }

    @Override
    public PageResult<MerchantListItemVO> pageMerchants(String keyword, long pageNo, long pageSize, String sort) {
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        Page<MerchantEntity> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<MerchantEntity> w = new LambdaQueryWrapper<>();
        w.eq(MerchantEntity::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            w.like(MerchantEntity::getName, keyword.trim());
        }
        applyMerchantSort(w, sort);
        merchantMapper.selectPage(page, w);

        PageResult<MerchantListItemVO> result = new PageResult<>();
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setTotal(page.getTotal());
        List<MerchantListItemVO> list = page.getRecords().stream().map(this::toListItem).collect(Collectors.toList());
        result.setRecords(list);
        return result;
    }

    @Override
    public MerchantListItemVO getMerchant(Long merchantId) {
        MerchantEntity entity = requireOpenMerchant(merchantId);
        return toListItem(entity);
    }

    @Override
    public List<ProductCategoryVO> listCategories(Long merchantId) {
        requireOpenMerchant(merchantId);
        LambdaQueryWrapper<ProductCategoryEntity> w = new LambdaQueryWrapper<>();
        w.eq(ProductCategoryEntity::getMerchantId, merchantId);
        w.orderByAsc(ProductCategoryEntity::getSort).orderByAsc(ProductCategoryEntity::getId);
        return productCategoryMapper.selectList(w).stream().map(this::toCategoryVo).collect(Collectors.toList());
    }

    @Override
    public List<ProductListItemVO> listProducts(Long merchantId, Long categoryId) {
        requireOpenMerchant(merchantId);
        LambdaQueryWrapper<ProductEntity> w = new LambdaQueryWrapper<>();
        w.eq(ProductEntity::getMerchantId, merchantId);
        w.eq(ProductEntity::getStatus, 1);
        if (categoryId != null) {
            w.eq(ProductEntity::getCategoryId, categoryId);
        }
        w.orderByAsc(ProductEntity::getCategoryId).orderByAsc(ProductEntity::getId);
        return productMapper.selectList(w).stream().map(this::toProductVo).collect(Collectors.toList());
    }

    private MerchantEntity requireOpenMerchant(Long merchantId) {
        MerchantEntity entity = merchantMapper.selectById(merchantId);
        if (entity == null || entity.getStatus() == null || entity.getStatus() != 1) {
            throw new BizException(40404, "merchant not found");
        }
        return entity;
    }

    private ProductCategoryVO toCategoryVo(ProductCategoryEntity e) {
        ProductCategoryVO vo = new ProductCategoryVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setSort(e.getSort());
        return vo;
    }

    private ProductListItemVO toProductVo(ProductEntity e) {
        ProductListItemVO vo = new ProductListItemVO();
        vo.setId(e.getId());
        vo.setCategoryId(e.getCategoryId());
        vo.setName(e.getName());
        vo.setDescription(e.getDescription());
        vo.setPrice(e.getPrice());
        vo.setStock(e.getStock());
        vo.setSales(e.getSales());
        return vo;
    }

    private MerchantListItemVO toListItem(MerchantEntity e) {
        MerchantListItemVO vo = new MerchantListItemVO();
        vo.setId(e.getId());
        vo.setName(e.getName());
        vo.setLogoUrl(e.getLogoUrl());
        vo.setMinDeliveryAmount(e.getMinDeliveryAmount());
        vo.setDeliveryFee(e.getDeliveryFee());
        vo.setRating(e.getRating());
        vo.setMonthlySales(e.getMonthlySales());
        vo.setDistanceKm(e.getDistanceKm());
        return vo;
    }
}
