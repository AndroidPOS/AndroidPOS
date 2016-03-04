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
	 * 値引き額(単位パイサ)
	 */
	private long discountPaisa;

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
		this.discountPaisa = 0;
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

	public void setDiscountValue(long discountValue) {
		this.discountPaisa = discountValue;
	}

	public long getDiscountValue() {
		return discountPaisa;
	}

	/**
	 * 値引き額の総額を、個々のオーダーに割り振る処理。
	 * これを通さないと、DBに割引額が反映されない。
	 */
	public void calcDiscountAllocation() {
		// discountValue = 0なら値引きしてないが、更新に対応するため全部の値引き額を0にリセット
		if (discountPaisa <= 0) {
			for (Order order : orders) {
				order.setDiscount(0);
			}
			return;
		}

		//　利益が無い＝次に個々のOrderの値引き割合を計算できない＝既に値引き処理済とみなして何もしない
		long revenue = getTotalRevenue();
		if (revenue <= 0) {
			return;
		}

		BigDecimal discountDecimal = new BigDecimal(discountPaisa);
		BigDecimal revenueDecimal = new BigDecimal(revenue);

		BigDecimal downPercentDecimal = discountDecimal.divide(revenueDecimal, 7, BigDecimal.ROUND_HALF_UP);
		long totalDiscount = 0;

		for (Order order : orders) {
			BigDecimal orderRevenue = new BigDecimal(order.getRevenue(false));
			//
			long orderDiscount = orderRevenue.multiply(downPercentDecimal).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
			Log.d("debug", "discount set " + order.getProductName() + " :" + orderDiscount);
			order.setDiscount(orderDiscount);
			totalDiscount += orderDiscount;
		}

		Log.d("debug", "discount total=" + totalDiscount + "/ set=" + discountPaisa);

		// 端数がある場合、差額分だけ先頭Orderの値引き額を変動させる
		if (totalDiscount != discountPaisa) {
			orders.get(0).setDiscount(orders.get(0).getDiscount() - (totalDiscount - discountPaisa));
		}
	}

	/**
	 * この販売の売り上げ金額を返す
	 * @return long 売り上げ(単位パイサ)
	 */
	public long getTotalSales() {
		long totalSales = 0;
		for (Order order : orders) {
			totalSales += order.getTotalAmount();
		}
		return totalSales;
	}

	/**
	 * この販売の原価を計算する
	 * @return long 原価(単位パイサ)
	 */
	public long getTotalCost() {
		long totalCost = 0;
		for (Order order : orders) {
			totalCost += order.getTotalCost();
		}
		return totalCost;
	}

	/**
	 * この販売による利益（売り上げ-原価）を返す。このメソッドは値引きを考慮しない
	 * @return long 利益金額(単位パイサ)
	 */
	public long getTotalRevenue() {
		return (getTotalSales() - getTotalCost());
	}

	public Date getSalesDate() {
		return salesDate;
	}

	/**
	 * この販売のお客さんの属性を設定する
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
