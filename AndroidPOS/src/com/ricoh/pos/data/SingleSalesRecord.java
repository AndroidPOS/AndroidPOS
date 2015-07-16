package com.ricoh.pos.data;

import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;


/**
 * 販売1件を表すクラス。
 * この中に複数のOrderが設定される
 */
public class SingleSalesRecord {

	private long id;

	/**
	 * 販売日時
	 */
	private Date salesDate;

	/**
	 * 値引き額
	 */
	private double discountValue;

	/**
	 * ユーザー属性
	 */
	private String userAttribute;

	/**
	 * Orderの配列
	 */
	private ArrayList<Order> orders;

	public SingleSalesRecord(Date date) {
		orders = new ArrayList<Order>();
		this.salesDate = date;
		this.discountValue = 0;
	}

	public void setOrders(ArrayList<Order> orderList) {
		this.orders = orderList;
	}

	public ArrayList<Order> getAllOrders() {
		return orders;
	}

	public void addOrder(Order order) {
		if (order == null) {
			throw new IllegalArgumentException("Order is null");
		}
		orders.add(order);
	}

	public void setDiscountValue(double discountValue) {
		this.discountValue = discountValue;
	}

	public double getDiscountValue() {
		return discountValue;
	}

	/**
	 * 値引き額の総額を、個々のオーダーに割り振る処理。
	 * これを通さないと、DBに割引額が反映されない。
	 */
	public void calcDiscountAllocation() {
		// discountValue = 0なら値引きしてないのでなにもしない
		//　利益が無い＝既に値引き処理済とみなして何もしない
		if (getTotalRevenue() <= 0 || discountValue <= 0) {
			return;
		}

		double downPercent = discountValue / getTotalRevenue();
		double totalDiscount = 0.0;

		for (Order order : orders) {
			// 小数2位で丸める
			BigDecimal big = new BigDecimal(order.getRevenue(false) * downPercent);
			double discount = big.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

			Log.d("debug", "discount set " + order.getProductName() + " :" + discount);
			order.setDiscount(discount);
			totalDiscount += discount;
		}

		Log.d("debug", "discount total=" + totalDiscount + "/ set=" + discountValue);

		// 端数がある場合の対処
		if (totalDiscount != discountValue) {
			// totalの方が大きい＝実際より大きく値引き額を設定＝値引き額を減らす必要がある
			BigDecimal fraction = new BigDecimal(orders.get(0).getDiscount() - (totalDiscount - discountValue));
			orders.get(0).setDiscount(fraction.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}
	}

	/**
	 * この販売の売り上げ金額を返す
	 *
	 * @return
	 */
	public double getTotalSales() {
		double totalSales = 0;
		for (Order order : orders) {
			totalSales += order.getTotalAmount();
		}
		return totalSales;
	}

	/**
	 * この販売の原価を計算する
	 *
	 * @return double 原価
	 */
	public double getTotalCost() {
		double totalCost = 0;
		for (Order order : orders) {
			totalCost += order.getTotalCost();
		}
		return totalCost;
	}

	/**
	 * この販売による利益（売り上げ-原価）を返す。このメソッドは値引きを考慮しない
	 *
	 * @return double 利益金額
	 */
	public double getTotalRevenue() {
		return getTotalSales() - getTotalCost();
	}

	public Date getSalesDate() {
		return salesDate;
	}

	/**
	 * この販売のお客さんの属性を設定する
	 *
	 * @param attribute 属性文字列
	 */
	public void setUserAttribute(String attribute) {
		if (attribute == null || attribute.length() == 0) {
			throw new IllegalArgumentException("User attribute is illegal");
		}
		this.userAttribute = attribute;
	}

	/**
	 * この販売のお客さんの属性を返す
	 *
	 * @return String
	 */
	public String getUserAttribute() {
		return userAttribute;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}
