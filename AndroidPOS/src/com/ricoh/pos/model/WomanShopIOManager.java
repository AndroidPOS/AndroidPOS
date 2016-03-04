package com.ricoh.pos.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.ricoh.pos.data.Product;
import com.ricoh.pos.data.WomanShopDataDef;
import com.ricoh.pos.DatabaseHelper;
import com.ricoh.pos.data.WomanShopFormatter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class WomanShopIOManager implements IOManager {

	private SQLiteDatabase database;
	private static String csvStorageFolder = "/Ricoh";
	private static final String defaultStock="0";

	private final String[] WomanShopDataStructure = new String[]{
			WomanShopDataDef.PRODUCT_CODE.name(),
			WomanShopDataDef.ITEM_CATEGORY.name(),
			WomanShopDataDef.PRODUCT_CATEGORY.name(),
			WomanShopDataDef.SALE_PRICE.name(),
			WomanShopDataDef.COST_TO_ENTREPRENEUR.name(),
			WomanShopDataDef.STOCK.name()};

	public WomanShopIOManager() {
		// Nothing to do
	}

	@Override
	public List<Product> searchAll() {
		Cursor cursor = null;

		try {
			cursor = database.query(
					DatabaseHelper.PRODUCT_DB,
					WomanShopDataStructure,
					null, null, null, null, null);
			Log.d("debug", "count:" + cursor.getCount());

			List<Product> products = new ArrayList<Product>();
			while (cursor.moveToNext()) {
				products.add(readCursor(cursor));
			}

			return products;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public void updateStock(String productCode, String productStock) {
		ContentValues values = new ContentValues();
		values.put(WomanShopDataDef.STOCK.name(), productStock);
		String[] arg = {productCode};
		database.update(DatabaseHelper.PRODUCT_DB, values, WomanShopDataDef.PRODUCT_CODE.name() + "=?", arg);
	}

	public void importCSV() throws IOException {
		File original = getArrivedGoods();
		File backup = backupArrivedGoods(original);

		BufferedReader reader = null;
		boolean isDataFail = false;

		try {
            reader = new BufferedReader(new FileReader(backup));

            // skipping header
            String header = reader.readLine();
            // import に失敗した行だけをもとの CSV ファイルに残す。そのために一度中身をヘッダだけにしてある
            FileUtils.write(original, header + "\n", DEFAULT_CHARSET, false);
            // adding arrived goods to DB
            String line;

            while ((line = reader.readLine()) != null) {

                String[] split = line.split(",");
                if (split.length != 5 && split.length != 6) {
                    Log.e("error", "This line is NOT adequate format");
                    FileUtils.write(original, line + "\n", DEFAULT_CHARSET, true);
                    isDataFail = true;
                    continue;
                }

                String code = split[WomanShopDataDef.PRODUCT_CODE.ordinal()];
                String name = split[WomanShopDataDef.ITEM_CATEGORY.ordinal()];
                String category = split[WomanShopDataDef.PRODUCT_CATEGORY.ordinal()];

                try {
                    // CSVはルピー単位の表記なので、パイサ単位の整数に変換
					long originalCost = WomanShopFormatter.convertRupeeToPaisa(split[WomanShopDataDef.COST_TO_ENTREPRENEUR.ordinal()]);
					long price = WomanShopFormatter.convertRupeeToPaisa(split[WomanShopDataDef.SALE_PRICE.ordinal()]);

                    // stockのカラムがない場合は0で設定
                    String stockStr = split.length == 6 ? split[WomanShopDataDef.STOCK.ordinal()] : defaultStock;
                    int stock = Integer.valueOf(stockStr);
                    Product product = new Product(code, name, category, originalCost, price, stock);

                    stock(product);
                } catch (NumberFormatException e) {
                    Log.e("error", "failed to convert into double or int.", e);
                    FileUtils.write(original, line + "\n", DEFAULT_CHARSET, true);
                    isDataFail = true;
                } catch (IllegalArgumentException e) {
                    Log.e("error", "conflicted with a record in DB.", e);
                    FileUtils.write(original, line + "\n", DEFAULT_CHARSET, true);
                    isDataFail = true;
                }
            }

            if (isDataFail) {
                throw new IllegalArgumentException();
            }
		} finally {
			IOUtils.closeQuietly(reader);
		}
    }

	public Product searchById(String productId) {
		Cursor cursor = null;

		try {
			String[] args = {productId};
			cursor = database.query(
					DatabaseHelper.PRODUCT_DB,
					WomanShopDataStructure,
					WomanShopDataDef.PRODUCT_CODE.name() + "=?",
					args,
					null, null, null);

			if (cursor.moveToNext()) {
				return readCursor(cursor);
			}

		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}

	/**
	 * 新しい ID の商品ならinsert, 既存の商品なら 入荷数を在庫に追加。
	 * ID が同じでも価格等の他のカラムが違っていれば、壊れているデータなので、例外を投げる
	 *
	 * @param arrivedProduct
	 */
	private void stock(Product arrivedProduct) {

		Log.d("debug", "stocking" + arrivedProduct);

		Product productInDb = searchById(arrivedProduct.getCode());

		if (productInDb == null) {
			Log.d("debug", arrivedProduct.getCode() + " is new product.");

			ContentValues contentValue = new ContentValues();
			contentValue.put(WomanShopDataDef.PRODUCT_CODE.name(), arrivedProduct.getCode());
			contentValue.put(WomanShopDataDef.PRODUCT_CATEGORY.name(), arrivedProduct.getCategory());
			contentValue.put(WomanShopDataDef.ITEM_CATEGORY.name(), arrivedProduct.getName());
			contentValue.put(WomanShopDataDef.COST_TO_ENTREPRENEUR.name(), arrivedProduct.getOriginalCost());
			contentValue.put(WomanShopDataDef.SALE_PRICE.name(), arrivedProduct.getPrice());
			contentValue.put(WomanShopDataDef.STOCK.name(), arrivedProduct.getStock());

			database.insertWithOnConflict(DatabaseHelper.PRODUCT_DB, null, contentValue,
					SQLiteDatabase.CONFLICT_REPLACE);

		} else {
			if (productInDb.equals(arrivedProduct)) {
				Log.d("debug", "restocking " + arrivedProduct.getCode());

				ContentValues contentValue = new ContentValues();
				contentValue.put(WomanShopDataDef.STOCK.name(), productInDb.getStock() + arrivedProduct.getStock());
				String[] args = {arrivedProduct.getCode()};
				database.update(DatabaseHelper.PRODUCT_DB, contentValue, WomanShopDataDef.PRODUCT_CODE.name() + "=?", args);

			} else {
				throw new IllegalArgumentException("Conflicted Product. DB:" + productInDb + ", Arrived Product:" + arrivedProduct);
			}
		}
	}

	private File getArrivedGoods() throws FileNotFoundException {
		String csvStoragePath = getCSVStoragePath();
		return new File(csvStoragePath + "/Product.csv");
	}

	private File backupArrivedGoods(File original) throws IOException {
		Calendar now = Calendar.getInstance();
		int year = now.get(Calendar.YEAR);
		int month = now.get(Calendar.MONTH) + 1;
		int day = now.get(Calendar.DAY_OF_MONTH);
		int hour = now.get(Calendar.HOUR_OF_DAY);
		int minute = now.get(Calendar.MINUTE);
		int second = now.get(Calendar.SECOND);
		String csvStoragePath = getCSVStoragePath();
		File backup = new File(csvStoragePath + "/" + year + "-" + month + "-" + day + "_" + hour + "-" + minute + "-" + second + "_" + "Product.csv");
		FileUtils.copyFile(original, backup);
		return backup;
	}

	/**
	 * debug用にカラム名を出力する
	 *
	 * @param record CSVの一行目の文字列
	 */
	private void writeFieldName(String record) {
		String[] fieldNames = record.split(",");
		for (String fieldName : fieldNames) {
			Log.d("debug", fieldName);
		}
	}

	// TODO: Add this function to interface
	public void setDatabase(SQLiteDatabase database) {
		if (this.database == null) {
			Log.d("debug", "Database set:" + DatabaseHelper.PRODUCT_DB);
			this.database = database;
		}
	}

	// TODO: Add this function to interface
	public void closeDatabase() {
		if (database != null) {
			Log.d("debug", "Database closed:" + DatabaseHelper.PRODUCT_DB);
			database.close();
		}
	}

	private Product readCursor(Cursor cursor) {
		int indexProductCode = cursor.getColumnIndex(WomanShopDataDef.PRODUCT_CODE.name());
		int indexItemName = cursor.getColumnIndex(WomanShopDataDef.ITEM_CATEGORY.name());
		int indexProductCategory = cursor.getColumnIndex(WomanShopDataDef.PRODUCT_CATEGORY.name());
		int indexCostToEntrepreneur = cursor.getColumnIndex(WomanShopDataDef.COST_TO_ENTREPRENEUR.name());
		int indexSalePrice = cursor.getColumnIndex(WomanShopDataDef.SALE_PRICE.name());
		int indexStock = cursor.getColumnIndex(WomanShopDataDef.STOCK.name());

		String productCode = cursor.getString(indexProductCode);
		String productCategory = cursor.getString(indexProductCategory);
		String itemName = cursor.getString(indexItemName);
		long salePrice = cursor.getLong(indexSalePrice);
		long costToEntrepreneur = cursor.getLong(indexCostToEntrepreneur);
		int stock = cursor.getInt(indexStock);

		return new Product(productCode, itemName, productCategory, costToEntrepreneur, salePrice, stock);
	}

	public void updateStock(Product product, int decrement) {
		Product productInDb = searchById(product.getCode());
		int stock = productInDb.getStock() - decrement;
		ContentValues contentValue = new ContentValues();
		contentValue.put(WomanShopDataDef.STOCK.name(), stock > 0 ? stock : 0);
		String[] args = {productInDb.getCode()};
		database.update(DatabaseHelper.PRODUCT_DB, contentValue, WomanShopDataDef.PRODUCT_CODE.name() + "=?", args);
	}

	public String getCSVStoragePath() {
		File exterlStorage = Environment.getExternalStorageDirectory();
		Log.d("debug", "Environment External:" + exterlStorage.getAbsolutePath());
		return exterlStorage.getAbsolutePath() + csvStorageFolder;
	}
}
