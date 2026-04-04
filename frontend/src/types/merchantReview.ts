export interface ReviewRecommendProduct {
  productId: number;
  productName: string;
}

export interface MerchantReviewItem {
  id: number;
  userId: number;
  userDisplayName: string;
  /** 个人主页「我的评价」列表会返回 */
  merchantId?: number;
  merchantName?: string;
  createdAt: string;
  rating: number;
  content: string;
  imageUrls: string[];
  recommendProducts: ReviewRecommendProduct[];
}

export interface MerchantReviewPage {
  records: MerchantReviewItem[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export interface MerchantReviewCreatePayload {
  merchantId: number;
  rating: number;
  content: string;
  imageUrls: string[];
  recommendProductIds: number[];
}
