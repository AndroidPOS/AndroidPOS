package com.ricoh.pos;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ricoh.pos.data.WomanShopDataDef;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String PRODUCT_DB = "products_dummy";


	public DatabaseHelper(Context context) {
		super(context, PRODUCT_DB + ".db", null, 2);

	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("Create Table " + PRODUCT_DB + " (" + "_id Integer Primary Key, "
				+ WomanShopDataDef.PRODUCT_CODE.name() + " Text UNIQUE, "
				+ WomanShopDataDef.PRODUCT_CATEGORY.name() + " Text, "
				+ WomanShopDataDef.ITEM_CATEGORY.name() + " Text, "
				+ WomanShopDataDef.STOCK.name() + " Integer, "
				+ WomanShopDataDef.SALE_PRICE.name() + " Real, "
				+ WomanShopDataDef.COST_TO_ENTREPRENEUR.name() + " Real)");
		Log.d("debug", "Database onCreate");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2) {
			db.execSQL("ALTER TABLE " + PRODUCT_DB + " ADD " + WomanShopDataDef.STOCK.name() + " INTEGER NOT NULL DEFAULT 0;");
		}
	}

}