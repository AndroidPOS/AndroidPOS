package com.ricoh.pos.data;

public class Order {

	private Product product;
	private int num;

	// この商品売り上げにおける割引額。単位パイサ。ルピー表記する場合は100で割る
	// 割引額はSingleSalesRecord1件につき１つ設定されて、そこから個々のオーダーに具体的な額が割り当てられる。
	private long discountValue;

	public Order(Product product, int numberOfOrder) {
		if (product == null || numberOfOrder < 0) {
			throw new IllegalArgumentException();
		}
		setOrder(product, numberOfOrder);
	}

	public void setOrder(Product product, int numberOfOrder) {
		if (product == null || numberOfOrder < 0) {
			throw new IllegalArgumentException();
		}

		this.product = product;
		this.num = numberOfOrder;
	}

	public void setNumberOfOrder(int num) {
		if (num < 0) {
			throw new IllegalArgumentException("Number of order should be positive");
		}

		this.num = num;
	}

	public void plusNumberOfOrder() {
		num++;
	}

	public void minusNumberOfOrder() {
		if (num == 0) {

		} else if (num > 0) {
			num--;
		} else {
			throw new IllegalStateException("number of order is illegal");
		}
	}

	public int getNumberOfOrder() {
		return num;
	}

	public Product getProduct() {
		return product;
	}

	public String getProductCode() {
		return product.getCode();
	}

	public String getProductCategory() {
		return product.getCategory();
	}

	public String getProductName() {
		return product.getName();
	}

	public long getProductPrice() {
		return product.getPrice();
	}

	public long getTotalAmount() {
		return product.getPrice() * num;
	}

	public long getTotalCost() {
		return product.getOriginalCost() * num;
	}

	public long getRevenue(boolean enableDisCount) {
		long result = (product.getPrice() - product.getOriginalCost()) * num;
		if (enableDisCount) {
			return (result - discountValue);
		}
		return result;
	}

	public void setDiscount(long discount) {
		discountValue = discount;
	}

	public long getDiscount() {
		return discountValue;
	}

	protected boolean equals(String productName) {
		return productName.equals(product.getName());
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("Order{");
		sb.append("product=").append(product == null ? "null" : product);
		sb.append(", num=").append(num);
		sb.append(", discountValue=").append(WomanShopFormatter.convertPaisaToRupee(discountValue));
		sb.append("}");
		return sb.toString();
	}
}
