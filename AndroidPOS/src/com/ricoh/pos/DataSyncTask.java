package com.ricoh.pos;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.ricoh.pos.data.Product;
import com.ricoh.pos.model.ProductsManager;
import com.ricoh.pos.model.WomanShopIOManager;
import com.ricoh.pos.model.WomanShopSalesIOManager;

import java.io.IOException;
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
		boolean isDataFail = false;

		// CSV から入荷商品を読み込んで DB に登録。
		try {
			womanShopIOManager.importCSV();
		} catch (IOException e) {
			Log.d("debug", "import error", e);
			isImportFail = true;
		}catch (IllegalArgumentException e) {
			Log.d("debug", "import error partially", e);
			isDataFail = true;
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

		List<Product> productsInDb = womanShopIOManager.searchAll();
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
		if (isDataFail) {
			return AsyncTaskResult.createErrorResult(R.string.data_error);
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