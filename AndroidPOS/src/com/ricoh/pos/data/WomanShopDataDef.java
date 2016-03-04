package com.ricoh.pos.data;

public enum WomanShopDataDef {
	PRODUCT_CODE("product_id"),
	ITEM_CATEGORY("product_name"),
	PRODUCT_CATEGORY("category_name"),
	COST_TO_ENTREPRENEUR("unit_selling_price"),
	SALE_PRICE("MRP"),
	STOCK("stock");

	private String name;
	WomanShopDataDef(String name){ this.name = name;}
	public String getName() {return name;}
}
