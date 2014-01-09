package com.ricoh.pos.model;

import java.util.ArrayList;

import com.ricoh.pos.data.Order;
import com.ricoh.pos.data.Product;

public class RegisterManager {
	
	private static RegisterManager instance;
	
	private ArrayList<Order> orderList;
	
	private ArrayList<UpdateOrderListener> listeners;
	
	private double discountValue;
	
	private RegisterManager(){
		orderList = new ArrayList<Order>();
		listeners = new ArrayList<UpdateOrderListener>();
	}
	
	public static RegisterManager getInstance(){
		if (instance == null) {
			instance = new RegisterManager();
		}
		
		return instance;
	}
	
	public void updateOrder(Product product, int num){
		Order orderOfTheProduct = findOrderOfTheProduct(product);
		
		if (orderOfTheProduct == null) {
			Order newOrder = new Order(product,num);
			orderList.add(newOrder);
		} else {
			orderOfTheProduct.setNumberOfOrder(num);
		}
		
		notifyUpdateOrder(product);
	}
	
	public void plusNumberOfOrder(Product product){
		Order orderOfTheProduct = findOrderOfTheProduct(product);
		
		if (orderOfTheProduct == null) {
			int numberOfFirstOrder = 1;
			Order newOrder = new Order(product,numberOfFirstOrder);
			orderList.add(newOrder);
		} else {
			orderOfTheProduct.plusNumberOfOrder();
		}
		
		notifyUpdateOrder(product);
	}
	
	public void minusNumberOfOrder(Product product) {
		Order orderOfTheProduct = findOrderOfTheProduct(product);
		
		if (orderOfTheProduct == null) {
			Order newOrder = new Order(product,0);
			orderList.add(newOrder);
		} else {
			orderOfTheProduct.minusNumberOfOrder();
		}
		
		notifyUpdateOrder(product);
	}
	
	private void notifyUpdateOrder(Product product){
		
		if (listeners == null || listeners.isEmpty()) {
			throw new IllegalStateException("UpdateOrderListener is not resgistered");
		}
		
		for (UpdateOrderListener listener : listeners) {
			if (listener == null) {
				throw new IllegalStateException("UpdateOrderListener to register is null");
			}
			listener.notifyUpdateOrder(getTotalAmount());
		}
	}
	
	public double getTotalAmount(){
		double totalAmount = 0;
		for (Order order: orderList) {
			totalAmount += order.getTotalAmount();
		}
		totalAmount -= discountValue;
		return totalAmount;
	}
	
	public void clearAllOrders(){
		orderList = new ArrayList<Order>();
		discountValue = 0;
	}
	
	public Order findOrderOfTheProduct(Product product){
		for (Order order : orderList) {
			if ((order.getProductCategory().equals(product.getCategory()) && order.getProductName().equals(product.getName()))) {
				return order;
			}
		}
		// Not Found
		return null;
	}
	
	public int getNumberOfOrder(Product product) {
		Order order = findOrderOfTheProduct(product);
		return order.getNumberOfOrder();
	}
	
	public void setUpdateOrderListener(UpdateOrderListener listener){
		listeners.add(listener);
	}
	
	public void removeUpdateOrderListener(UpdateOrderListener listener){
		listeners.remove(listener);
	}
	
	public void clearUpdateOrderListener(){
		listeners.clear();
	}
	
	public void updateDiscountValue(double discountValue)
	{
		this.discountValue = discountValue;
		notifyUpdateOrder(null);
	}
}
