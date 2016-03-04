package com.ricoh.pos.model;

import android.util.Log;

import com.ricoh.pos.data.Order;
import com.ricoh.pos.data.OrderUpdateInfo;
import com.ricoh.pos.data.Product;
import com.ricoh.pos.data.SingleSalesRecord;
import com.ricoh.pos.data.WomanShopFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

public class RegisterManager {

	private static RegisterManager instance;

	private ArrayList<Order> orderList;

	private ArrayList<UpdateOrderListener> listeners;
	private ArrayList<UpdateOrderListListener> orderListListeners;

	private long discountValuePaisa;	// この販売の値引き額。単位パイサ。

	private String userAttribute;

	private RegisterManager() {
		orderList = new ArrayList<Order>();
		listeners = new ArrayList<UpdateOrderListener>();
		orderListListeners = new ArrayList<UpdateOrderListListener>();
	}

	public static RegisterManager getInstance() {
		if (instance == null) {
			instance = new RegisterManager();
		}
		return instance;
	}

	public void updateOrder(Product product, int num) {
		Order orderOfTheProduct = findOrderOfTheProduct(product);

		if (num == 0) {
			if (orderOfTheProduct == null) {
				return;
			} else {
				orderList.remove(orderOfTheProduct);
			}
		} else if (orderOfTheProduct == null) {
			Order newOrder = new Order(product, num);
			orderList.add(newOrder);
		} else {
			orderOfTheProduct.setNumberOfOrder(num);
		}

		notifyUpdateOrder();
	}

	public void plusNumberOfOrder(Product product) {
		Order orderOfTheProduct = findOrderOfTheProduct(product);

		if (orderOfTheProduct == null) {
			int numberOfFirstOrder = 1;
			Order newOrder = new Order(product, numberOfFirstOrder);
			orderList.add(newOrder);
		} else {
			orderOfTheProduct.plusNumberOfOrder();
		}

		notifyUpdateOrder();
	}

	public void minusNumberOfOrder(Product product) {
		Order orderOfTheProduct = findOrderOfTheProduct(product);

		if (orderOfTheProduct != null) {
			orderOfTheProduct.minusNumberOfOrder();

			// 注文が0になったら削除
			if (orderOfTheProduct.getNumberOfOrder() == 0) {
				orderList.remove(orderOfTheProduct);
			}
		}

		notifyUpdateOrder();
	}

	private void notifyUpdateOrder() {

		if (listeners == null || listeners.isEmpty()) {
			throw new IllegalStateException("UpdateOrderListener is not resgistered");
		}

		for (UpdateOrderListener listener : listeners) {
			if (listener == null) {
				throw new IllegalStateException("UpdateOrderListener to register is null");
			}
			listener.notifyUpdateOrder(new OrderUpdateInfo(getOriginalTotalAmount()
					, discountValuePaisa
					, getTotalAmountAfterDiscount()));
		}
	}

	public void notifyUpdateOrderList() {
		if (orderListListeners == null || orderListListeners.isEmpty()) {
			throw new IllegalStateException("UpdateOrderListListener is not resgistered");
		}

		for (UpdateOrderListListener listener : orderListListeners) {
			if (listener == null) {
				throw new IllegalStateException("UpdateOrderListListener to register is null");
			}
			listener.notifyUpdateOrderList();
		}
	}

	public long getOriginalTotalAmount() {
		long totalAmount = 0;
		for (Order order : orderList) {
			totalAmount += order.getTotalAmount();
		}
		return totalAmount;
	}

	public long getOriginalTotalCost() {
		long totalCost = 0;
		for (Order order : orderList) {
			totalCost += order.getTotalCost();
		}
		return totalCost;
	}

	public long getTotalAmountAfterDiscount() {
		return getOriginalTotalAmount() - discountValuePaisa;
	}

	public int getTotalNumberOfOrder() {
		int totalNumber = 0;
		for (Order order : orderList) {
			totalNumber += order.getNumberOfOrder();
		}
		return totalNumber;
	}

	public void clearAllOrders() {
		orderList = new ArrayList<Order>();
		discountValuePaisa = 0;
		userAttribute = null;
	}

	public Order findOrderOfTheProduct(Product product) {
		for (Order order : orderList) {
			if ((order.getProductCategory().equals(product.getCategory()) && order.getProductCode().equals(product.getCode()))) {
				return order;
			}
		}
		// Not Found
		return null;
	}

	public int getNumberOfOrder(Product product) {
		Order order = findOrderOfTheProduct(product);
		if (order == null) {
			return 0;
		} else {
			return order.getNumberOfOrder();
		}
	}

	public void setUpdateOrderListener(UpdateOrderListener listener) {
		listeners.add(listener);
	}

	public void removeUpdateOrderListener(UpdateOrderListener listener) {
		listeners.remove(listener);
	}

	public void clearUpdateOrderListener() {
		listeners.clear();
	}

	public void setUpdateOrderListListener(UpdateOrderListListener listener) {
		orderListListeners.add(listener);
	}

	public void removeUpdateOrderListListener(UpdateOrderListListener listener) {
		orderListListeners.remove(listener);
	}

	public void clearUpdateOrderListListener() {
		orderListListeners.clear();
	}

	/**
	 * 値引きを設定する。
	 * @param discountPisa 値引き額.単位パイサ
	 */
	public void updateDiscountValue(long discountPisa) {
		this.discountValuePaisa = discountPisa;
		try {
			if (discountValuePaisa >= (getOriginalTotalAmount() - getOriginalTotalCost())) {
				throw new IllegalArgumentException("discountValues is larger than totalCost");
			}
		} finally {
			notifyUpdateOrder();
		}
	}

	public void setUserAttribute(String attribute) {
		if (attribute == null || attribute.length() == 0) {
			throw new IllegalArgumentException("User attribute is illegal");
		}
		this.userAttribute = attribute;
		Log.d("Attribute:", " " + userAttribute + " ");
	}

	public SingleSalesRecord getSingleSalesRecord() {
		SingleSalesRecord record = new SingleSalesRecord(new Date());
		record.setOrders(orderList);
		record.setDiscountValue(discountValuePaisa);
		record.setUserAttribute(userAttribute);
		return record;
	}

	public void clearDiscountValue() {
		this.discountValuePaisa = 0;
	}

}
