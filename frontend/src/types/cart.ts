export interface CartItem {
  merchantId: number;
  merchantName: string | null;

  productId: number;
  productName: string;

  price: number;
  quantity: number;
  subtotal: number;
}

