package com.food.delivery.impl.merchant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.delivery.common.exception.BizException;
import com.food.delivery.dto.merchant.MerchantReviewCreateDTO;
import com.food.delivery.entity.merchant.MerchantEntity;
import com.food.delivery.entity.product.ProductEntity;
import com.food.delivery.entity.review.MerchantReviewEntity;
import com.food.delivery.entity.review.MerchantReviewImageEntity;
import com.food.delivery.entity.review.MerchantReviewRecommendEntity;
import com.food.delivery.entity.user.UserEntity;
import com.food.delivery.mapper.merchant.MerchantMapper;
import com.food.delivery.mapper.product.ProductMapper;
import com.food.delivery.mapper.review.MerchantReviewImageMapper;
import com.food.delivery.mapper.review.MerchantReviewMapper;
import com.food.delivery.mapper.review.MerchantReviewRecommendMapper;
import com.food.delivery.mapper.user.UserMapper;
import com.food.delivery.service.merchant.MerchantReviewService;
import com.food.delivery.vo.common.PageResult;
import com.food.delivery.vo.merchant.MerchantReviewVO;
import com.food.delivery.vo.merchant.ReviewRecommendProductVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MerchantReviewServiceImpl implements MerchantReviewService {

    private final MerchantReviewMapper reviewMapper;
    private final MerchantReviewImageMapper imageMapper;
    private final MerchantReviewRecommendMapper recommendMapper;
    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public MerchantReviewServiceImpl(
            MerchantReviewMapper reviewMapper,
            MerchantReviewImageMapper imageMapper,
            MerchantReviewRecommendMapper recommendMapper,
            MerchantMapper merchantMapper,
            ProductMapper productMapper,
            UserMapper userMapper
    ) {
        this.reviewMapper = reviewMapper;
        this.imageMapper = imageMapper;
        this.recommendMapper = recommendMapper;
        this.merchantMapper = merchantMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<MerchantReviewVO> pageReviews(Long merchantId, long pageNo, long pageSize) {
        if (merchantId == null || merchantId < 1) {
            throw new BizException(40001, "invalid merchant");
        }
        requireOpenMerchant(merchantId);
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        Page<MerchantReviewEntity> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<MerchantReviewEntity> w = new LambdaQueryWrapper<>();
        w.eq(MerchantReviewEntity::getMerchantId, merchantId);
        w.orderByDesc(MerchantReviewEntity::getCreatedAt);
        reviewMapper.selectPage(page, w);

        List<MerchantReviewEntity> rows = page.getRecords();
        if (rows == null || rows.isEmpty()) {
            PageResult<MerchantReviewVO> empty = new PageResult<>();
            empty.setPageNo(pageNo);
            empty.setPageSize(pageSize);
            empty.setTotal(page.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        List<Long> reviewIds = rows.stream().map(MerchantReviewEntity::getId).filter(Objects::nonNull).toList();
        Set<Long> userIds = rows.stream().map(MerchantReviewEntity::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, List<String>> imagesByReview = loadImages(reviewIds);
        Map<Long, List<MerchantReviewRecommendEntity>> recByReview = loadRecommends(reviewIds);
        Set<Long> allProductIds = recByReview.values().stream()
                .flatMap(List::stream)
                .map(MerchantReviewRecommendEntity::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> productNameById = loadProductNames(allProductIds);
        Map<Long, UserEntity> userById = loadUsers(userIds);

        MerchantEntity merchantRow = merchantMapper.selectById(merchantId);
        String merchantName =
                merchantRow != null && merchantRow.getName() != null ? merchantRow.getName() : "";

        List<MerchantReviewVO> vos = new ArrayList<>();
        for (MerchantReviewEntity e : rows) {
            MerchantReviewVO vo = new MerchantReviewVO();
            vo.setId(e.getId());
            vo.setUserId(e.getUserId());
            vo.setMerchantId(e.getMerchantId());
            vo.setMerchantName(merchantName);
            vo.setCreatedAt(e.getCreatedAt());
            vo.setRating(e.getRating());
            vo.setContent(e.getContent());
            vo.setImageUrls(imagesByReview.getOrDefault(e.getId(), Collections.emptyList()));
            UserEntity u = userById.get(e.getUserId());
            vo.setUserDisplayName(displayName(u));
            List<ReviewRecommendProductVO> recommend = new ArrayList<>();
            for (MerchantReviewRecommendEntity rr : recByReview.getOrDefault(e.getId(), Collections.emptyList())) {
                ReviewRecommendProductVO rp = new ReviewRecommendProductVO();
                rp.setProductId(rr.getProductId());
                rp.setProductName(productNameById.getOrDefault(rr.getProductId(), ""));
                recommend.add(rp);
            }
            vo.setRecommendProducts(recommend);
            vos.add(vo);
        }

        PageResult<MerchantReviewVO> result = new PageResult<>();
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setTotal(page.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Override
    public PageResult<MerchantReviewVO> pageReviewsByUser(Long userId, long pageNo, long pageSize) {
        if (userId == null || userId < 1) {
            throw new BizException(40001, "invalid user");
        }
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        Page<MerchantReviewEntity> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<MerchantReviewEntity> w = new LambdaQueryWrapper<>();
        w.eq(MerchantReviewEntity::getUserId, userId);
        w.orderByDesc(MerchantReviewEntity::getCreatedAt);
        reviewMapper.selectPage(page, w);

        List<MerchantReviewEntity> rows = page.getRecords();
        if (rows == null || rows.isEmpty()) {
            PageResult<MerchantReviewVO> empty = new PageResult<>();
            empty.setPageNo(pageNo);
            empty.setPageSize(pageSize);
            empty.setTotal(page.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        List<Long> reviewIds = rows.stream().map(MerchantReviewEntity::getId).filter(Objects::nonNull).toList();
        Set<Long> merchantIds =
                rows.stream().map(MerchantReviewEntity::getMerchantId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> merchantNames = loadMerchantNames(merchantIds);
        Map<Long, List<String>> imagesByReview = loadImages(reviewIds);
        Map<Long, List<MerchantReviewRecommendEntity>> recByReview = loadRecommends(reviewIds);
        Set<Long> allProductIds = recByReview.values().stream()
                .flatMap(List::stream)
                .map(MerchantReviewRecommendEntity::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> productNameById = loadProductNames(allProductIds);
        Set<Long> userIds = Collections.singleton(userId);
        Map<Long, UserEntity> userById = loadUsers(userIds);

        List<MerchantReviewVO> vos = new ArrayList<>();
        for (MerchantReviewEntity e : rows) {
            MerchantReviewVO vo = new MerchantReviewVO();
            vo.setId(e.getId());
            vo.setUserId(e.getUserId());
            vo.setMerchantId(e.getMerchantId());
            vo.setMerchantName(merchantNames.getOrDefault(e.getMerchantId(), ""));
            vo.setCreatedAt(e.getCreatedAt());
            vo.setRating(e.getRating());
            vo.setContent(e.getContent());
            vo.setImageUrls(imagesByReview.getOrDefault(e.getId(), Collections.emptyList()));
            UserEntity u = userById.get(e.getUserId());
            vo.setUserDisplayName(displayName(u));
            List<ReviewRecommendProductVO> recommend = new ArrayList<>();
            for (MerchantReviewRecommendEntity rr : recByReview.getOrDefault(e.getId(), Collections.emptyList())) {
                ReviewRecommendProductVO rp = new ReviewRecommendProductVO();
                rp.setProductId(rr.getProductId());
                rp.setProductName(productNameById.getOrDefault(rr.getProductId(), ""));
                recommend.add(rp);
            }
            vo.setRecommendProducts(recommend);
            vos.add(vo);
        }

        PageResult<MerchantReviewVO> result = new PageResult<>();
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setTotal(page.getTotal());
        result.setRecords(vos);
        return result;
    }

    private Map<Long, String> loadMerchantNames(Set<Long> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<MerchantEntity> list = merchantMapper.selectBatchIds(merchantIds);
        Map<Long, String> map = new HashMap<>();
        for (MerchantEntity m : list) {
            if (m != null && m.getId() != null) {
                map.put(m.getId(), m.getName() != null ? m.getName() : "");
            }
        }
        return map;
    }

    private Map<Long, List<String>> loadImages(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<MerchantReviewImageEntity> w = new LambdaQueryWrapper<>();
        w.in(MerchantReviewImageEntity::getReviewId, reviewIds);
        w.orderByAsc(MerchantReviewImageEntity::getSortOrder).orderByAsc(MerchantReviewImageEntity::getId);
        List<MerchantReviewImageEntity> list = imageMapper.selectList(w);
        Map<Long, List<String>> map = new HashMap<>();
        for (MerchantReviewImageEntity img : list) {
            if (img.getReviewId() == null || !StringUtils.hasText(img.getImageUrl())) {
                continue;
            }
            map.computeIfAbsent(img.getReviewId(), k -> new ArrayList<>()).add(img.getImageUrl());
        }
        return map;
    }

    private Map<Long, List<MerchantReviewRecommendEntity>> loadRecommends(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<MerchantReviewRecommendEntity> w = new LambdaQueryWrapper<>();
        w.in(MerchantReviewRecommendEntity::getReviewId, reviewIds);
        List<MerchantReviewRecommendEntity> list = recommendMapper.selectList(w);
        Map<Long, List<MerchantReviewRecommendEntity>> map = new HashMap<>();
        for (MerchantReviewRecommendEntity r : list) {
            map.computeIfAbsent(r.getReviewId(), k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    private Map<Long, String> loadProductNames(Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductEntity> products = productMapper.selectBatchIds(productIds);
        Map<Long, String> map = new HashMap<>();
        for (ProductEntity p : products) {
            if (p != null && p.getId() != null) {
                map.put(p.getId(), p.getName());
            }
        }
        return map;
    }

    private Map<Long, UserEntity> loadUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        Map<Long, UserEntity> map = new HashMap<>();
        for (UserEntity u : users) {
            if (u != null && u.getId() != null) {
                map.put(u.getId(), u);
            }
        }
        return map;
    }

    private static String displayName(UserEntity u) {
        if (u == null) {
            return "用户";
        }
        if (StringUtils.hasText(u.getNickname())) {
            return u.getNickname().trim();
        }
        return maskMobile(u.getMobile());
    }

    private static String maskMobile(String mobile) {
        if (!StringUtils.hasText(mobile) || mobile.length() < 7) {
            return "用户";
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReview(Long userId, MerchantReviewCreateDTO dto) {
        if (userId == null || userId < 1) {
            throw new BizException(40100, "unauthorized");
        }
        Long merchantId = dto.getMerchantId();
        requireOpenMerchant(merchantId);

        List<Long> productIds = dto.getRecommendProductIds() == null
                ? Collections.emptyList()
                : dto.getRecommendProductIds().stream().filter(Objects::nonNull).distinct().toList();
        if (productIds.size() > 8) {
            throw new BizException(40001, "推荐菜过多");
        }
        for (Long pid : productIds) {
            ProductEntity p = productMapper.selectById(pid);
            if (p == null || p.getMerchantId() == null || !p.getMerchantId().equals(merchantId)) {
                throw new BizException(40001, "推荐商品不属于该商家");
            }
        }

        List<String> urls = dto.getImageUrls() == null
                ? Collections.emptyList()
                : dto.getImageUrls().stream().filter(StringUtils::hasText).map(String::trim).toList();
        if (urls.size() > 6) {
            throw new BizException(40001, "图片过多");
        }

        LocalDateTime now = LocalDateTime.now();
        MerchantReviewEntity rev = new MerchantReviewEntity();
        rev.setMerchantId(merchantId);
        rev.setUserId(userId);
        rev.setRating(dto.getRating());
        rev.setContent(dto.getContent().trim());
        rev.setCreatedAt(now);
        rev.setUpdatedAt(now);
        reviewMapper.insert(rev);
        Long reviewId = rev.getId();

        int sort = 0;
        for (String url : urls) {
            MerchantReviewImageEntity img = new MerchantReviewImageEntity();
            img.setReviewId(reviewId);
            img.setImageUrl(url);
            img.setSortOrder(sort++);
            imageMapper.insert(img);
        }
        for (Long pid : productIds) {
            MerchantReviewRecommendEntity rr = new MerchantReviewRecommendEntity();
            rr.setReviewId(reviewId);
            rr.setProductId(pid);
            recommendMapper.insert(rr);
        }
        return reviewId;
    }

    private void requireOpenMerchant(Long merchantId) {
        MerchantEntity m = merchantMapper.selectById(merchantId);
        if (m == null || m.getStatus() == null || m.getStatus() != 1) {
            throw new BizException(40404, "merchant not found");
        }
    }
}
