package com.ricoh.pos;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ricoh.pos.data.Product;
import com.ricoh.pos.data.WomanShopDataDef;
import com.ricoh.pos.model.ProductsManager;
import com.ricoh.pos.model.WomanShopIOManager;
import com.ricoh.pos.model.WomanShopSalesIOManager;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * 同期処理の本体
 */
public class DataSyncTask extends AsyncTask<String, Void, AsyncTaskResult<String>> {
	final String TAG = "DataSyncTask";
	DataSyncTaskCallback callback;
	Context context;
	ProgressDialog progressDialog;
	WomanShopIOManager womanShopIOManager;
	ProductsManager productsManager;

	public DataSyncTask(Context context, DataSyncTaskCallback callback, WomanShopIOManager womanShopIOManager) {
		this.callback = callback;
		this.context = context;
		this.womanShopIOManager = womanShopIOManager;
		this.productsManager = ProductsManager.getInstance();
	}

	@Override
	protected void onPreExecute() {
		Log.d(TAG, "onPreExecute");
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(context);
			progressDialog.setMessage(context.getString(R.string.dialog_data_syncro_message));
			progressDialog.show();
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.setCancelable(false);
		}
	}

	@Override
	protected AsyncTaskResult<String> doInBackground(String... params) {
		Log.d(TAG, "doInBackground");
		Log.d("debug", "SyncButton click");
		boolean isImportFail = false;
		boolean isExportFail = false;

		// CSV から入荷商品を読み込んで DB に登録。
		try {
			File original = womanShopIOManager.getArrivedGoods();
			File backup = womanShopIOManager.backupArrivedGoods(original);

			//TODO ここでエラーになったらエラーフラグを立ててエラーメッセージを出す
			if (original == null || !original.exists() || backup == null || !backup.exists()) {
				String originalFileStatus = original == null ? "null" : String.valueOf(original.exists());
				String backupFileStatus = original == null ? "null" : String.valueOf(original.exists());
				Log.d("debug", "Original File:" + originalFileStatus + " / Backup File:" + backupFileStatus);
				return AsyncTaskResult.createErrorResult(R.string.sd_import_error);
			}

			// import に失敗した行だけを CSV ファイルに記録する。そのために一度ファイルごと削除
			FileUtils.forceDelete(original);

			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(backup));

				// skipping header
				String header = reader.readLine();
				String[] fieldNames = header.split(",");
				for (String fieldName : fieldNames) {
					Log.d("debug", fieldName);
				}

				// inserting arrived goods to DB
				String line;
				while ((line = reader.readLine()) != null) {

					String[] split = line.split(",");
					String code = split[WomanShopDataDef.PRODUCT_CODE.ordinal()];
					String name = split[WomanShopDataDef.ITEM_CATEGORY.ordinal()];
					String category = split[WomanShopDataDef.PRODUCT_CATEGORY.ordinal()];
					double originalCost = Double.valueOf(split[WomanShopDataDef.COST_TO_ENTREPRENEUR.ordinal()]);
					double price = Double.valueOf(split[WomanShopDataDef.SALE_PRICE.ordinal()]);
					int stock = Integer.valueOf(split[WomanShopDataDef.STOCK.ordinal()]);

					Product product = new Product(code, name, category, originalCost, price, stock);

					try {
						womanShopIOManager.stock(product);

					} catch (IllegalArgumentException e) {
						Log.e("error", "conflicted with a record in DB.", e);
						FileUtils.write(original, line + "\n", Charset.forName("UTF-8"), true);
					}
				}

			} finally {
				if (reader != null) {
					reader.close();
				}
			}


		} catch (IOException e) {
			Log.d("debug", "import error", e);
			isImportFail = true;
		}

		// 販売履歴DBの内容をexportする
		try {
			WomanShopSalesIOManager.getInstance().exportCSV(this.context);
		} catch (IOException e) {
			Log.d("debug", "export error", e);
			isExportFail = true;
		}

		// 在庫DBを検索して、画面表示用にデータモデルを生成する。
		// やらないと画面が表示できないので、エラーの有無に関わらず実施。

		List<Product> productsInDb = womanShopIOManager.searchAlldata();
		productsManager.updateProducts(productsInDb);

		if (isImportFail && isExportFail) {
			return AsyncTaskResult.createErrorResult(R.string.sd_import_export_error);
		}
		if (isExportFail) {
			return AsyncTaskResult.createErrorResult(R.string.sd_export_error);
		}
		if (isImportFail) {
			return AsyncTaskResult.createErrorResult(R.string.sd_import_error);
		}
		// The argument is null because nothing to notify on success
		return AsyncTaskResult.createNormalResult(null);
	}

	@Override
	protected void onPostExecute(AsyncTaskResult<String> result) {
		Log.d(TAG, "onPostExecute");
		if (progressDialog.isShowing()) {
			progressDialog.dismiss();

			if (result.isError()) {
				callback.onFailedSyncData(result.getResourceId());
			} else {
				callback.onSuccessSyncData();
			}
		}
	}
}