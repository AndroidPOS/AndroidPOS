package com.ricoh.pos.model;

import android.util.Log;

import com.ricoh.pos.data.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProductsManager {

	private HashMap<String, ArrayList<Product>> productsMap;
	private static ProductsManager instance;

	private ProductsManager() {
		this.productsMap = new HashMap<String, ArrayList<Product>>();
	}

	public static ProductsManager getInstance() {
		if (instance == null) {
			instance = new ProductsManager();
		}
		return instance;
	}

	public void updateProducts(List<Product> products) {
		productsMap.clear();
		for (Product product : products) {
			Log.d("debug", product.toString());
			addNewProductInCategory(product.getCategory(), product);
		}
	}

	public void addNewProductInCategory(String category, Product product) {

		if (category == null || category.length() == 0) {
			throw new IllegalArgumentException("Invalid category");
		}

		if (product == null) {
			throw new IllegalArgumentException("Passing product object is null");
		}

		if (this.productsMap.containsKey(category)) {
			ArrayList<Product> produtcsInCategory = productsMap.get(category);
			for (Product registeredProduct : produtcsInCategory) {
				if (registeredProduct.equals(product)) {
					throw new IllegalArgumentException("Conflicted Product:" + registeredProduct + "," + product);
				}
			}
			produtcsInCategory.add(product);
		} else {
			ArrayList<Product> productsInCategory = new ArrayList<Product>();
			productsInCategory.add(product);
			productsMap.put(category, productsInCategory);
		}
	}

	public Product getProductByName(String category, String productName) {
		if (productName == null || productName.length() == 0) {
			throw new IllegalArgumentException("Invalid product name");
		}
		if (category == null || category.length() == 0) {
			throw new IllegalArgumentException("Invalid category name");
		}

		if (!productsMap.containsKey(category)) {
			throw new IllegalArgumentException("Passing category does not exist: " + category);
		}

		ArrayList<Product> productList = productsMap.get(category);
		for (Product product : productList) {
			if (product.getName().equals(productName)) {
				// Successfully found
				return product;
			}
		}
		// Not Found
		return null;
	}

	public Product getProductFromCode(String category, String productCode) {
		if (category == null || category.length() == 0) {
			throw new IllegalArgumentException("Invalid category name");
		}
		if (productCode == null || productCode.length() == 0) {
			throw new IllegalArgumentException("Invalid productCode name");
		}

		if (!productsMap.containsKey(category)) {
			throw new IllegalArgumentException("Passing category does not exist: " + category);
		}

		ArrayList<Product> productList = productsMap.get(category);
		for (Product product : productList) {
			if (product.getCode().equals(productCode)) {
				// Successfully found
				return product;
			}
		}
		// Not Found
		return null;
	}

	public ArrayList<Product> getProductsInCategory(String category) {

		if (category == null || category.length() == 0) {
			throw new IllegalArgumentException("Invalid category name");
		}

		if (!productsMap.containsKey(category)) {
			throw new IllegalArgumentException("Passing category does not exist: " + category);
		} else {
			return productsMap.get(category);
		}
	}

	public ArrayList<Product> getAllProducts() {

		ArrayList<Product> allProducts = new ArrayList<Product>();
		for (String key : productsMap.keySet()) {
			allProducts.addAll(productsMap.get(key));
		}
		return allProducts;
	}

	public int getNumberOfProductsInCategory(String category) {

		if (category == null || category.length() == 0) {
			throw new IllegalArgumentException("Invalid category name");
		}

		if (!productsMap.containsKey(category)) {
			throw new IllegalArgumentException("Passing category does not exist: " + category);
		} else {
			return productsMap.get(category).size();
		}
	}

	public String[] getAllCategoryName() {
		int categoryCount = getCategoryCount();
		if (categoryCount <= 0) {
			throw new NegativeArraySizeException("Category does not exist: " + categoryCount);
		}

		String[] results = new String[getCategoryCount()];
		int i = 0;
		for (HashMap.Entry<String, ArrayList<Product>> keyValue : productsMap.entrySet()) {
			results[i++] = keyValue.getKey();
		}
		return results;
	}

	public int getCategoryCount() {
		return productsMap.entrySet().size();
	}
}
