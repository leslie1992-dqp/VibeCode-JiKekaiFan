export interface MerchantListItem {
  id: number;
  name: string;
  logoUrl: string | null;
  minDeliveryAmount: number;
  deliveryFee: number;
  rating: number;
  monthlySales: number;
  /** 演示：相对距离 km */
  distanceKm?: number | null;
}

export interface MerchantPage {
  records: MerchantListItem[];
  total: number;
  pageNo: number;
  pageSize: number;
}

export interface ProductCategory {
  id: number;
  name: string;
  sort: number;
}

export interface ProductListItem {
  id: number;
  categoryId: number;
  name: string;
  description: string | null;
  price: number;
  stock: number;
  sales: number;
}

export interface SeckillCoupon {
  id: number;
  merchantId: number;
  title: string;
  thresholdAmount: number;
  discountAmount: number;
  stockRemain: number;
  validFrom: string;
  validUntil: string;
}
