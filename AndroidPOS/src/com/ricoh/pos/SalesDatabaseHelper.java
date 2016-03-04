package com.ricoh.pos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import com.ricoh.pos.data.Order;
import com.ricoh.pos.data.Product;
import com.ricoh.pos.data.SingleSalesRecord;
import com.ricoh.pos.data.WomanShopFormatter;
import com.ricoh.pos.data.WomanShopSalesDef;
import com.ricoh.pos.data.WomanShopSalesOrderDef;
import com.ricoh.pos.model.WomanShopSalesIOManager;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


/**
 * 販売実績DBの生成用ヘルパー
 * ２つのテーブルを生成する。
 */
public class SalesDatabaseHelper extends SQLiteOpenHelper {
	public static final String SALES_DB_NAME = "sales.db";
	public static final String WS_SALES_TABLE_NAME = "tbl_ws_sales";
	public static final String WS_SALES_ORDER_TABLE_NAME = "tbl_ws_sales_order";

	public SalesDatabaseHelper(Context context) {
		super(context, SALES_DB_NAME, null, 3);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("Create Table " + WS_SALES_TABLE_NAME + " ("
						+ "_id Integer Primary Key, "
						+ WomanShopSalesDef.DATE.name() + " Text, "
						+ WomanShopSalesDef.DISCOUNT.name() + " Integer INTEGER NOT NULL DEFAULT 0, "
						+ WomanShopSalesDef.USER_AGES.name() + " Text)"
		);
		Log.d("debug", "SingleSales Table onCreate");

		db.execSQL("Create Table " + WS_SALES_ORDER_TABLE_NAME + " ("
						+ "_id Integer Primary Key, "
						+ WomanShopSalesOrderDef.PRODUCT_CODE.name() + " Text, "
						+ WomanShopSalesOrderDef.PRODUCT_NAME.name() + " Text, "
						+ WomanShopSalesOrderDef.CATEGORY_NAME.name() + " Text, "
						+ WomanShopSalesOrderDef.PURCHASE_PRICE.name() + " Integer, "
						+ WomanShopSalesOrderDef.UNIT_PRICE.name() + " Integer, "
						+ WomanShopSalesOrderDef.QTY.name() + " Integer, "
						+ WomanShopSalesOrderDef.DISCOUNT.name() + " Integer INTEGER NOT NULL DEFAULT 0, "
						+ WomanShopSalesOrderDef.SINGLE_SALES_ID.name() + " Integer)"
		);
		Log.d("debug", "SingleSalesOrder table onCreate");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < newVersion) {
			// Version2->3の時にテーブルを大きく書き換えたため、Version1->2の時のDB変更処理も、
			// Version2->3の処理の一部として処理できる。
			if (oldVersion < 3) {
				this.convertDoubleToLong(db, oldVersion);

				// 以前のDBで値がヒンドゥー語の場合に英語に変換
				String[] findKeys = new String[]{"10 साल से कम","10 साल से ज्यादा","20 से 29 के बीच","30 से 39 के बीच","40 से 49 के बीच","50 से 59 के बीच","60 से 70 के बीच","अन्य"};
				String[] results = new String[]{"10s Low","10s High","20s","30s","40s","50s","60s","Other"};
				
				for(int i = 0; i<findKeys.length; i++){
					ContentValues value = new ContentValues();
					value.put(WomanShopSalesDef.USER_AGES.name(),results[i]);
					db.update(WS_SALES_TABLE_NAME, value, WomanShopSalesDef.USER_AGES.name() + " = ?", new String[]{findKeys[i]});
				}
			}
		}
	}

	// スキーマ変更用の旧データ構造
	class OldProduct {
		public String code;
		public String category;
		public String name;
		public double originalCost;    // 原価。
		public double price;            // 販売価格。
		public int stock;
	}
	class OldOrder {
		public OldProduct product;
		public int num;
		public double discountValue;
	}
	class OldSalesRecord {
		public long id;
		public Date salesDate;
		public double discount;
		public String userAttribute;
		public ArrayList<OldOrder> orders;
	}

	private void convertDoubleToLong(SQLiteDatabase db, int oldVersion)
	{
		ArrayList<OldSalesRecord> oldSales = this.dumpOldData(db, oldVersion);
		this.alterTable(db);
		this.importOldData(db, oldSales, oldVersion);
	}

	private ArrayList<OldSalesRecord> dumpOldData(SQLiteDatabase db, int oldVerison)
	{
		String[] oldJoinedTable = new String[]{
				SalesDatabaseHelper.WS_SALES_TABLE_NAME + "." + WomanShopSalesDef.DATE.name(),
				SalesDatabaseHelper.WS_SALES_TABLE_NAME + "." + WomanShopSalesDef.DISCOUNT.name(),
				SalesDatabaseHelper.WS_SALES_TABLE_NAME + "." + WomanShopSalesDef.USER_AGES.name(),
				SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.PRODUCT_CODE.name(),
				SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.CATEGORY_NAME.name(),
				SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.PRODUCT_NAME.name(),
				SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.PURCHASE_PRICE.name(),
				SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.UNIT_PRICE.name(),
				SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.QTY.name(),
				SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.DISCOUNT.name(),
				SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.SINGLE_SALES_ID.name()
		};

		int dateColumnNo = 0;
		int discountColumnNo = 1;
		int userAgesColumnNo = 2;
		int productCodeColumnNo = 3;
		int categoryColumnNo = 4;
		int prodctNameColumnNo = 5;
		int purchasePriceColumnNo = 6;
		int unitPriceColumnNo = 7;
		int qtyColumnNo = 8;
		int orderDiscountColumnNo = 9;
		int singleSalesIdColumnNo = 10;

		Cursor cursor = null;
		ArrayList<OldSalesRecord> oldSales = new ArrayList<OldSalesRecord>();

		try {
			// 最初期のDBの場合は、まずカラムを足して構造をバージョン２にあわせる
			if (oldVerison == 1) {
				db.execSQL("alter table " + WS_SALES_ORDER_TABLE_NAME
						+ " add " + WomanShopSalesOrderDef.DISCOUNT.name() + " Integer INTEGER NOT NULL DEFAULT 0");
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

			SQLiteQueryBuilder query = new SQLiteQueryBuilder();
			query.setTables(SalesDatabaseHelper.WS_SALES_TABLE_NAME +
							" left join " +
							SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME +
							" on " +
							SalesDatabaseHelper.WS_SALES_TABLE_NAME +
							".ROWID = "	+ oldJoinedTable[10]
			);

			cursor = query.query(db, oldJoinedTable, null, null, null, null, null, null);

			OldSalesRecord record = null;
			int salesId = -1;
			boolean recordAvailable = cursor.moveToFirst();
			while (recordAvailable) {
				if (salesId != cursor.getInt(10)) {
					if (record != null) {
						oldSales.add(record);
					}

					salesId = cursor.getInt(singleSalesIdColumnNo);

					record = new OldSalesRecord();
					record.orders = new ArrayList<OldOrder>();
					record.id = salesId;
					record.salesDate = dateFormat.parse(cursor.getString(dateColumnNo));
					record.discount = cursor.getDouble(discountColumnNo);
					record.userAttribute = cursor.getString(userAgesColumnNo);
				}

				OldProduct product = new OldProduct();
				product.code = cursor.getString(productCodeColumnNo);
				product.name = cursor.getString(prodctNameColumnNo);
				product.category = cursor.getString(categoryColumnNo);
				product.originalCost = cursor.getDouble(purchasePriceColumnNo);
				product.price = cursor.getDouble(unitPriceColumnNo);

				OldOrder order = new OldOrder();
				order.product = product;
				order.num = cursor.getInt(qtyColumnNo);
				order.discountValue = cursor.getDouble(orderDiscountColumnNo);

				record.orders.add(order);
				recordAvailable = cursor.moveToNext();
			}

			// 最後の1件を追加
			if (record != null) {
				oldSales.add(record);
			}
		} catch (ParseException pe) {
			Log.d("debug", "salseDate parse fail.", pe);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return oldSales;
	}

	private void alterTable(SQLiteDatabase db)
	{
		// drop
		db.execSQL("DROP TABLE " + WS_SALES_TABLE_NAME);
		db.execSQL("DROP TABLE " + WS_SALES_ORDER_TABLE_NAME);

		// create
		this.onCreate(db);
	}

	private void importOldData(SQLiteDatabase db, ArrayList<OldSalesRecord> oldSales, int oldVersion)
	{
		WomanShopSalesIOManager manager = WomanShopSalesIOManager.getInstance();
		manager.setDatabase(db);

		for (OldSalesRecord oldRecord : oldSales) {
			// create SingleRecord
			SingleSalesRecord newRecord = new SingleSalesRecord(oldRecord.salesDate);
			newRecord.setUserAttribute(oldRecord.userAttribute);

			long discount = WomanShopFormatter.convertRupeeToPaisa(oldRecord.discount);
			newRecord.setDiscountValue(discount);

			ArrayList<Order> newOrders = this.createNewOrder(oldRecord);
			newRecord.setOrders(newOrders);

			// oldVersionが1の時は、order単位のdiscountは初期値の0のままのはずなので、再配分する。
			// 2以上の時は、oldOrder.discountValueに配分済みの値がdoubleで格納されているはずなので、
			// createNewOrder()で初期化されている。
			if (oldVersion == 1) {
				newRecord.calcDiscountAllocation();
			}

			manager.insertSalesRecord(newRecord);
		}
	}

	private ArrayList<Order> createNewOrder(OldSalesRecord oldRecord)
	{
		ArrayList<Order> newOrders = new ArrayList<Order>();

		for (OldOrder oldOrder : oldRecord.orders) {
			OldProduct oldProduct = oldOrder.product;
			long originalCost = WomanShopFormatter.convertRupeeToPaisa(oldProduct.originalCost);
			long price = WomanShopFormatter.convertRupeeToPaisa(oldProduct.price);
			long orderDiscount = WomanShopFormatter.convertRupeeToPaisa(oldOrder.discountValue);

			Order order = new Order(
				new Product(oldProduct.code, oldProduct.name, oldProduct.category, originalCost, price, oldProduct.stock),
				oldOrder.num
			);
			order.setDiscount(orderDiscount);

			newOrders.add(order);
		}
		return newOrders;
	}
}