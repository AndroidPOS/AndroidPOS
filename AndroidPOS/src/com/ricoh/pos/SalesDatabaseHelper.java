package com.ricoh.pos;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ricoh.pos.data.Order;
import com.ricoh.pos.data.SingleSalesRecord;
import com.ricoh.pos.data.WomanShopSalesDef;
import com.ricoh.pos.data.WomanShopSalesOrderDef;
import com.ricoh.pos.model.WomanShopSalesIOManager;

import java.util.ArrayList;


/**
 * 販売実績DBの生成用ヘルパー
 * ２つのテーブルを生成する。
 */
public class SalesDatabaseHelper extends SQLiteOpenHelper {
	public static final String SALES_DB_NAME = "sales.db";
	public static final String WS_SALES_TABLE_NAME = "tbl_ws_sales";
	public static final String WS_SALES_ORDER_TABLE_NAME = "tbl_ws_sales_order";

	public SalesDatabaseHelper(Context context) {
		super(context, SALES_DB_NAME, null, 2);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < newVersion) {
			if (oldVersion == 1) {
				db.execSQL("alter table " + WS_SALES_ORDER_TABLE_NAME
						+ " add " + WomanShopSalesOrderDef.DISCOUNT.name() + " Real");

				WomanShopSalesIOManager womanShopSalesIoManager = WomanShopSalesIOManager.getInstance();
				womanShopSalesIoManager.setDatabase(db);
				ArrayList<SingleSalesRecord> sales = womanShopSalesIoManager.searchAll();
				for (SingleSalesRecord sale : sales) {
					if (sale.getDiscountValue() != 0) {
						sale.calcDiscountAllocation();
						ArrayList<Order> orders = sale.getAllOrders();
						for (Order order : orders) {
							ContentValues value = new ContentValues();
							value.put(WomanShopSalesOrderDef.DISCOUNT.name(), order.getDiscount());
							int update = db.update(WS_SALES_ORDER_TABLE_NAME, value,
									WomanShopSalesOrderDef.SINGLE_SALES_ID.name() + " = ? and "
											+ WomanShopSalesOrderDef.PRODUCT_CODE.name() + " = ?",
									new String[]{String.valueOf(sale.getId()), order.getProductCode()});
						}
					}
				}
			}
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("Create Table " + WS_SALES_TABLE_NAME + " ("
						+ "_id Integer Primary Key, "
						+ WomanShopSalesDef.DATE.name() + " Text, "
						+ WomanShopSalesDef.DISCOUNT.name() + " Real, "
						+ WomanShopSalesDef.USER_AGES.name() + " Text)"
		);
		Log.d("debug", "SingleSales Table onCreate");

		db.execSQL("Create Table " + WS_SALES_ORDER_TABLE_NAME + " ("
						+ "_id Integer Primary Key, "
						+ WomanShopSalesOrderDef.PRODUCT_CODE.name() + " Text, "
						+ WomanShopSalesOrderDef.PRODUCT_NAME.name() + " Text, "
						+ WomanShopSalesOrderDef.CATEGORY_NAME.name() + " Text, "
						+ WomanShopSalesOrderDef.PURCHASE_PRICE.name() + " Real, "
						+ WomanShopSalesOrderDef.UNIT_PRICE.name() + " Real, "
						+ WomanShopSalesOrderDef.QTY.name() + " Integer, "
						+ WomanShopSalesOrderDef.DISCOUNT.name() + " Real, "
						+ WomanShopSalesOrderDef.SINGLE_SALES_ID.name() + " Integer)"
		);
		Log.d("debug", "SingleSalesOrder table onCreate");
	}
}