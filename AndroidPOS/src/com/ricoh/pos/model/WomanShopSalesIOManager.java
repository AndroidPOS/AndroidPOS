package com.ricoh.pos.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.ricoh.pos.SalesDatabaseHelper;
import com.ricoh.pos.data.Order;
import com.ricoh.pos.data.Product;
import com.ricoh.pos.data.SingleSalesRecord;
import com.ricoh.pos.data.WomanShopFormatter;
import com.ricoh.pos.data.WomanShopSalesDef;
import com.ricoh.pos.data.WomanShopSalesOrderDef;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * 販売実績DBのコントローラークラス。元の名前はWomanShopSalesIOManager
 * 必要な操作は大体ここで出来るはず
 */
public class WomanShopSalesIOManager implements IOManager {
	/**
	 * ドリシティ向けに出力する販売実績CSVのファイル名
	 */
	private static final String SALES_CSV_FILE_NAME = "/sales.csv";

	/**
	 * 販売実績のCSV出力先フォルダ名
	 */
	private static final String CSV_STORAGE_FOLDER = "/Ricoh";

	/**
	 * DBに格納する時の日付けフォーマット
	 */
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * csvファイルのMIMEタイプ *
	 */
	private static final String CSV_MIME_TYPE = "text/comma-separated-values";

	//------------------------ private -----------------------------
	/**
	 * Singleton Instance
	 */
	private static WomanShopSalesIOManager instance = null;

	/**
	 * SQLite instance
	 */
	private SQLiteDatabase salesDatabase;

	private static class JoinedTable {
		public static class Sale {
			public static final int DATE = 0;
			public static final int DISCOUNT = 1;
			public static final int USER_AGES = 2;
		}

		public static class Order {
			private static final int PRODUCT_CODE = 3;
			private static final int CATEGORY_NAME = 4;
			private static final int PRODUCT_NAME = 5;
			private static final int PURCHASE_PRICE = 6;
			private static final int UNIT_PRICE = 7;
			private static final int QTY = 8;
			private static final int DISCOUNT = 9;
			private static final int SINGLE_SALES_ID = 10;
		}

		public static final String[] COLUMNS = new String[11];

		static {
			COLUMNS[0] = SalesDatabaseHelper.WS_SALES_TABLE_NAME + "." + WomanShopSalesDef.DATE.name();
			COLUMNS[1] = SalesDatabaseHelper.WS_SALES_TABLE_NAME + "." + WomanShopSalesDef.DISCOUNT.name();
			COLUMNS[2] = SalesDatabaseHelper.WS_SALES_TABLE_NAME + "." + WomanShopSalesDef.USER_AGES.name();
			COLUMNS[3] = SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.PRODUCT_CODE.name();
			COLUMNS[4] = SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.CATEGORY_NAME.name();
			COLUMNS[5] = SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.PRODUCT_NAME.name();
			COLUMNS[6] = SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.PURCHASE_PRICE.name();
			COLUMNS[7] = SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.UNIT_PRICE.name();
			COLUMNS[8] = SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.QTY.name();
			COLUMNS[9] = SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.DISCOUNT.name();
			COLUMNS[10] = SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME + "." + WomanShopSalesOrderDef.SINGLE_SALES_ID.name();
		}
	}

	// ------------------------ methods -----------------------------
	private WomanShopSalesIOManager() {
	}

	public static WomanShopSalesIOManager getInstance() {
		if (instance == null) {
			instance = new WomanShopSalesIOManager();
		}
		return instance;
	}

	/**
	 * デストラクタ代わりにインスタンスをnull化する。
	 */
	public static void removeInstance() {
		Log.d("debug", "Reset Instance:" + "WomanShopSalesIOManager");
		instance = null;
	}

	/**
	 * SalesDatabaseHelperからSQLiteのインスタンスを貰う。
	 * このクラスはSingletonなので、これが実質の初期化処理。これを実施しないと他のメソッドが機能しない。
	 *
	 * @param database Helperから貰うDBインスタンス
	 */
	public void setDatabase(SQLiteDatabase database) {
		// 既に開いているのがある場合、一度クローズ
		if (salesDatabase != null && salesDatabase.isOpen() && salesDatabase != database) {
			salesDatabase.close();
		}

		Log.d("debug", "Database set:" + SalesDatabaseHelper.SALES_DB_NAME);
		salesDatabase = database;
	}

	public void closeDatabase() {
		if (salesDatabase != null) {
			Log.d("debug", "Database closed:" + SalesDatabaseHelper.SALES_DB_NAME);
			salesDatabase.close();

		}
	}

	/**
	 * データ件数を返す
	 *
	 * @return 全データ件数
	 */
	public long getSalesCount() {
		return DatabaseUtils.queryNumEntries(salesDatabase, SalesDatabaseHelper.WS_SALES_TABLE_NAME);
	}

	/**
	 * DBから全件検索
	 *
	 * @return 検索結果のArrayList
	 */
	public ArrayList<SingleSalesRecord> searchAll() {
		Cursor cursor = null;

		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(
			SalesDatabaseHelper.WS_SALES_TABLE_NAME +
			" left join " +
			SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME +
			" on " +
			SalesDatabaseHelper.WS_SALES_TABLE_NAME +
			".ROWID = "	+ JoinedTable.COLUMNS[JoinedTable.Order.SINGLE_SALES_ID]
		);
		cursor = query.query(salesDatabase, JoinedTable.COLUMNS, null, null, null, null, null, null);

		ArrayList<SingleSalesRecord> sales = makeSalesList(cursor);
		cursor.close();
		return sales;
	}

	/**
	 * 日付けを指定して検索を実施
	 *
	 * @param date         日付け
	 * @param effectiveDay TRUEだと、時分秒を無視して、今日の範囲を検索、falseだと、時分秒まで検索
	 * @return 検索結果のArrayList
	 */
	public ArrayList<SingleSalesRecord> searchByDate(Date date, boolean effectiveDay) {
		Cursor cursor = null;

		SQLiteQueryBuilder query = new SQLiteQueryBuilder();
		query.setTables(
			SalesDatabaseHelper.WS_SALES_TABLE_NAME +
			" left join " +
			SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME +
			" on " +
			SalesDatabaseHelper.WS_SALES_TABLE_NAME +
			".ROWID = "	+ JoinedTable.COLUMNS[JoinedTable.Order.SINGLE_SALES_ID]
		);

		if (effectiveDay) {
			//今日の範囲で検索
			Calendar start = Calendar.getInstance();
			start.setTime(date);
			start.set(Calendar.HOUR, 0);
			start.set(Calendar.MINUTE, 0);
			start.set(Calendar.SECOND, 0);
			start.set(Calendar.MILLISECOND, 0);

			Calendar end = Calendar.getInstance();
			end.setTime(date);
			end.set(Calendar.HOUR, 23);
			end.set(Calendar.MINUTE, 59);
			end.set(Calendar.SECOND, 59);
			end.set(Calendar.MILLISECOND, 0);

			String[] params = {DATE_FORMAT.format(start.getTime()), DATE_FORMAT.format(end.getTime())};

			cursor = query.query(salesDatabase, JoinedTable.COLUMNS,
					JoinedTable.COLUMNS[JoinedTable.Sale.DATE] + " >= ? and " + JoinedTable.COLUMNS[JoinedTable.Sale.DATE] + "<= ?",
					params, null, null, null, null);
		} else {
			// 指定日の範囲を検索
			String[] params = {DATE_FORMAT.format(date)};
			cursor = query.query(salesDatabase, JoinedTable.COLUMNS,
					JoinedTable.COLUMNS[JoinedTable.Sale.DATE] + " = ?",
					params, null, null, null, null);
		}

		ArrayList<SingleSalesRecord> sales = makeSalesList(cursor);
		cursor.close();
		return sales;
	}

	private ArrayList<SingleSalesRecord> makeSalesList(Cursor cursor) {
		ArrayList<SingleSalesRecord> sales = new ArrayList<SingleSalesRecord>();

		SingleSalesRecord record = null;
		int salesId = -1;

		try {
			boolean recordAvailable = cursor.moveToFirst();

			while (recordAvailable) {
				if (salesId != cursor.getInt(JoinedTable.Order.SINGLE_SALES_ID)) {

					if (record != null) {
						sales.add(record);
					}

					salesId = cursor.getInt(JoinedTable.Order.SINGLE_SALES_ID);
					Date salesDate = DATE_FORMAT.parse(cursor.getString(JoinedTable.Sale.DATE));
					record = new SingleSalesRecord(salesDate);
					record.setId(salesId);
					record.setDiscountValue(cursor.getLong(JoinedTable.Sale.DISCOUNT));
					record.setUserAttribute(cursor.getString(JoinedTable.Sale.USER_AGES));
				}

				Product product = new Product(cursor.getString(JoinedTable.Order.PRODUCT_CODE),
						cursor.getString(JoinedTable.Order.PRODUCT_NAME),
						cursor.getString(JoinedTable.Order.CATEGORY_NAME),
						cursor.getLong(JoinedTable.Order.PURCHASE_PRICE),
						cursor.getLong(JoinedTable.Order.UNIT_PRICE));

				Order order = new Order(product, cursor.getInt(JoinedTable.Order.QTY));
				order.setDiscount(cursor.getLong(JoinedTable.Order.DISCOUNT));

				record.addOrder(order);

				recordAvailable = cursor.moveToNext();
			}


			// 最後の1件を追加
			if (record != null) {
				sales.add(record);
			}

		} catch (ParseException pe) {
			Log.d("debug", "salseDate parse fail.", pe);
		}
		return sales;
	}

	/**
	 * 決済が成立したら呼び出してDBに格納するメソッド
	 *
	 * @param record 　格納するデータを保持するインスタンス
	 */
	public void insertSalesRecord(SingleSalesRecord record) {
		ContentValues singleRecord = new ContentValues();
		salesDatabase.beginTransaction();

		try {
			String dateStr = DATE_FORMAT.format(record.getSalesDate());

			singleRecord.put(WomanShopSalesDef.DATE.name(), dateStr);
			singleRecord.put(WomanShopSalesDef.DISCOUNT.name(), record.getDiscountValue());
			singleRecord.put(WomanShopSalesDef.USER_AGES.name(), record.getUserAttribute());

			long rowID = salesDatabase.insertWithOnConflict(
					SalesDatabaseHelper.WS_SALES_TABLE_NAME,
					null,
					singleRecord,
					SQLiteDatabase.CONFLICT_REPLACE
			);

			ArrayList<Order> orders = record.getAllOrders();
			for (int i = 0; i < orders.size(); i++) {
				Order order = orders.get(i);
				Product product = order.getProduct();
				ContentValues orderRecord = new ContentValues();
				orderRecord.put(WomanShopSalesOrderDef.PRODUCT_CODE.name(), product.getCode());
				orderRecord.put(WomanShopSalesOrderDef.CATEGORY_NAME.name(), product.getCategory());
				orderRecord.put(WomanShopSalesOrderDef.PRODUCT_NAME.name(), product.getName());
				orderRecord.put(WomanShopSalesOrderDef.PURCHASE_PRICE.name(), product.getOriginalCost());
				orderRecord.put(WomanShopSalesOrderDef.UNIT_PRICE.name(), product.getPrice());
				orderRecord.put(WomanShopSalesOrderDef.QTY.name(), order.getNumberOfOrder());
				orderRecord.put(WomanShopSalesOrderDef.DISCOUNT.name(), order.getDiscount());
				orderRecord.put(WomanShopSalesOrderDef.SINGLE_SALES_ID.name(), rowID);

				salesDatabase.insertWithOnConflict(
						SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME,
						null,
						orderRecord,
						SQLiteDatabase.CONFLICT_REPLACE
				);
			}

			salesDatabase.setTransactionSuccessful();
		} finally {
			salesDatabase.endTransaction();
		}
	}

	/**
	 * 日付けで指定された売り上げデータを削除する。
	 *
	 * @param date 　キーになる日付けと時刻のデータ。時刻込みなのでユニークキーになるはず。
	 * @return 1なら削除成功。1以外だと想定外のエラー
	 */
	public int deleteSingleSalesRecordRelatedTo(Date date) {
		int deleted = 0;
		String dateStr = DATE_FORMAT.format(date);

		String[] params = {dateStr};
		salesDatabase.beginTransaction();

		Cursor cursor;
		try {
			// まず指定されたデータがあるかどうか
			String[] columns = {"ROWID"};
			String query = WomanShopSalesDef.DATE.name() + " = ?";
			cursor = salesDatabase.query(
					SalesDatabaseHelper.WS_SALES_TABLE_NAME,
					columns,
					query,
					params,
					null,
					null,
					null);

			if (cursor.getCount() != 1) {
				throw new SQLException("No data from SingleSalesTable");
			}

			cursor.moveToFirst();
			long id = cursor.getLong(0);
			cursor.close();

			// ここからdelete
			String[] deleteParams = new String[]{String.valueOf(id)};
			deleted = salesDatabase.delete(
					SalesDatabaseHelper.WS_SALES_TABLE_NAME, "ROWID=?", deleteParams
			);
			if (deleted <= 0) {
				throw new SQLException("delete fail from SingleSalesTable id=" + id);
			}

			int deleted2 = salesDatabase.delete(
					SalesDatabaseHelper.WS_SALES_ORDER_TABLE_NAME, WomanShopSalesOrderDef.SINGLE_SALES_ID.name() + " = ?", deleteParams
			);
			if (deleted2 <= 0) {
				throw new SQLException("delete fail from SingleSalesOrderTable id=" + id);
			}
			salesDatabase.setTransactionSuccessful();
		} catch (SQLException exp) {
			deleted = 0;
		} finally {
			salesDatabase.endTransaction();
		}

		return deleted;
	}

	public void exportCSV(Context context) throws IOException {
		try {
			// 全件検索してファイルに出力。
			// FIXME;全件でいいの？ある日付け以降とかでなく。
			ArrayList<SingleSalesRecord> records = this.searchAll();
			writeSalesData(context, records);
		} catch (IOException e) {
			Log.d("debug", "exportCSV Exception occuered.", e);
			throw e;
		}
	}

	private void writeSalesData(Context context, ArrayList<SingleSalesRecord> records) throws IOException {

		String csvStoragePath = getCSVStoragePath();
		File csvStorage = new File(csvStoragePath);

		if (!csvStorage.exists()) {
			Log.d("debug", "make directory:" + csvStoragePath);
			boolean result = csvStorage.mkdir();
			if (!result) {
				throw new IOException("Make csv storage directory fail.");
			}
		}

		File salesDataCSV = new File(csvStoragePath + SALES_CSV_FILE_NAME);
		FileOutputStream fos = null;
		OutputStreamWriter fileWriter = null;

		try {
			try {
				fos = new FileOutputStream(salesDataCSV);
				fos.write(0xef);
				fos.write(0xbb);
				fos.write(0xbf);
				fileWriter = new OutputStreamWriter(fos, DEFAULT_CHARSET);
			} catch (FileNotFoundException e) {
				Log.d("debug", "sales.csv is not found", e);
				throw e;
			} catch (UnsupportedEncodingException e) {
				Log.d("debug", DEFAULT_CHARSET.displayName() + " is unsupported", e);
				throw e;
			}

			try {
				for (SingleSalesRecord record : records) {
					ArrayList<Order> orders = record.getAllOrders();

					for (int orderNo = 0; orderNo < orders.size(); orderNo++) {
						Order order = orders.get(orderNo);
						Product product = order.getProduct();

						// CSV出力する際に、金額を単位変換する。
						String result = product.getCode() + ","
								+ product.getCategory() + ","
								+ product.getName() + ","
								+ order.getNumberOfOrder() + ","
								+ String.format("%.2f", WomanShopFormatter.convertPaisaToRupee(product.getPrice())) + ","
								+ String.format("%.2f", WomanShopFormatter.convertPaisaToRupee(product.getPrice() * order.getNumberOfOrder())) + ","
								+ String.format("%.2f", WomanShopFormatter.convertPaisaToRupee(order.getDiscount())) + ","
								+ record.getSalesDate().toString() + ","
								+ record.getUserAttribute() + "\n";

						fileWriter.write(result);
					}
				}
			} catch (IOException e) {
				Log.d("debug", "write error", e);
				throw e;
			}
		} finally {
			if (fileWriter != null) {
				fileWriter.flush();
				fileWriter.close();
			}
			if (fos != null) {
				fos.close();
			}

			MediaScannerConnection.OnScanCompletedListener mScanCompletedListener = new MediaScannerConnection.OnScanCompletedListener() {
				@Override
				public void onScanCompleted(String path, Uri uri) {
					Log.d("MediaScannerConnection", "Scanned " + path + ":");
					Log.d("MediaScannerConnection", "-> uri=" + uri);
				}
			};
			String[] paths = {Environment.getExternalStorageDirectory().getPath() + CSV_STORAGE_FOLDER + SALES_CSV_FILE_NAME};
			String[] mimeTypes = {CSV_MIME_TYPE};
			MediaScannerConnection.scanFile(context, paths, mimeTypes, mScanCompletedListener);
		}
	}

	public String getCSVStoragePath() {
		File extStorage = Environment.getExternalStorageDirectory();
		Log.d("debug", "Environment External:" + extStorage.getAbsolutePath());
		return extStorage.getAbsolutePath() + CSV_STORAGE_FOLDER;
	}
}
