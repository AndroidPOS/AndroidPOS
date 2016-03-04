package com.ricoh.pos;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;
import com.ricoh.pos.data.WomanShopDataDef;
import com.ricoh.pos.data.WomanShopFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
	public static final String PRODUCT_DB = "products_dummy";


	public DatabaseHelper(Context context) {
		super(context, PRODUCT_DB + ".db", null, 3);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("Create Table " + PRODUCT_DB + " (" + "_id Integer Primary Key, "
				+ WomanShopDataDef.PRODUCT_CODE.name() + " Text UNIQUE, "
				+ WomanShopDataDef.PRODUCT_CATEGORY.name() + " Text, "
				+ WomanShopDataDef.ITEM_CATEGORY.name() + " Text, "
				+ WomanShopDataDef.STOCK.name() + " Integer NOT NULL DEFAULT 0, "
				+ WomanShopDataDef.SALE_PRICE.name() + " Integer, "
				+ WomanShopDataDef.COST_TO_ENTREPRENEUR.name() + " Integer)");
		Log.d("debug", "Database onCreate");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			db.execSQL("ALTER TABLE " + PRODUCT_DB + " ADD " + WomanShopDataDef.STOCK.name() + " INTEGER NOT NULL DEFAULT 0;");

			// 桁を更新
			this.convertRealToIntetger(db);
		}
	}

	/**
	 *  BOP-66で誤差問題解消のためにDBをREALからintに変更する処理
	 * @param db
	 */
	private void convertRealToIntetger(SQLiteDatabase db)
	{
		Cursor cursor = null;
		class OldProduct {
			public String code;
			public String category;
			public String name;
			public double originalCost;    // 原価。
			public double price;            // 販売価格。
			public int stock;
		};

		try {
			String[] oldStructure = new String[]{
				WomanShopDataDef.PRODUCT_CODE.name(),
				WomanShopDataDef.ITEM_CATEGORY.name(),
				WomanShopDataDef.PRODUCT_CATEGORY.name(),
				WomanShopDataDef.SALE_PRICE.name(),
				WomanShopDataDef.COST_TO_ENTREPRENEUR.name(),
				WomanShopDataDef.STOCK.name(),
			};

			cursor = db.query(
					DatabaseHelper.PRODUCT_DB,
					oldStructure,
					null, null, null, null, null);

			ArrayList<OldProduct> products = new ArrayList<OldProduct>();
			while (cursor.moveToNext()) {
				OldProduct old = new OldProduct();
				old.code = cursor.getString(cursor.getColumnIndex(WomanShopDataDef.PRODUCT_CODE.name()));
				old.category = cursor.getString(cursor.getColumnIndex(WomanShopDataDef.PRODUCT_CATEGORY.name()));
				old.name = cursor.getString(cursor.getColumnIndex(WomanShopDataDef.ITEM_CATEGORY.name()));
				old.price = cursor.getDouble(cursor.getColumnIndex(WomanShopDataDef.SALE_PRICE.name()));
				old.originalCost = cursor.getDouble(cursor.getColumnIndex(WomanShopDataDef.COST_TO_ENTREPRENEUR.name()));
				int stock = cursor.getInt(cursor.getColumnIndex(WomanShopDataDef.STOCK.name()));
				products.add(old);
			}
			cursor.close();

			// drop
			db.execSQL("DROP TABLE " + PRODUCT_DB);
			// create
			this.onCreate(db);

			for (OldProduct old : products) {
				// 旧DBはルピー単位の表記なので、パイサ単位の整数に変換
				long originalCost = WomanShopFormatter.convertRupeeToPaisa(old.originalCost);
				long price = WomanShopFormatter.convertRupeeToPaisa(old.price);

				ContentValues contentValue = new ContentValues();
				contentValue.put(WomanShopDataDef.PRODUCT_CODE.name(), old.code);
				contentValue.put(WomanShopDataDef.PRODUCT_CATEGORY.name(), old.category);
				contentValue.put(WomanShopDataDef.ITEM_CATEGORY.name(), old.name);
				contentValue.put(WomanShopDataDef.STOCK.name(), old.stock);
				contentValue.put(WomanShopDataDef.SALE_PRICE.name(), price);
				contentValue.put(WomanShopDataDef.COST_TO_ENTREPRENEUR.name(), originalCost);
				db.insertWithOnConflict(DatabaseHelper.PRODUCT_DB, null, contentValue, SQLiteDatabase.CONFLICT_REPLACE);
			}

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

}