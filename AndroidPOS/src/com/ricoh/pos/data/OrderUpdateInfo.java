package com.ricoh.pos.data;

public class OrderUpdateInfo {
	private long totalAmountBeforeDiscount;	// 割引前の総額。単位パイサ。
	private long discountAmount;				// 割引額。単位パイサ。
	private long totalAmountAfterDiscount;		// 割引後の総額。単位パイサ。
	
	public OrderUpdateInfo(long totalAmountBeforeDiscount, long discountAmount, long totalAmountAfterDiscount) {
		this.totalAmountBeforeDiscount = totalAmountBeforeDiscount;
		this.discountAmount = discountAmount;
		this.totalAmountAfterDiscount = totalAmountAfterDiscount;
	}
	
	public long getTotalAmountBeforeDiscount() {
		return totalAmountBeforeDiscount;
	}
	
	public long getDiscountAmount() {
		return discountAmount;
	}
	
	public long getTotalAmountAfterDiscount() {
		return totalAmountAfterDiscount;
	}
}
