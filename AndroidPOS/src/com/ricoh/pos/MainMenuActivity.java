package com.ricoh.pos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.ricoh.pos.model.ProductsManager;
import com.ricoh.pos.model.WomanShopIOManager;
import com.ricoh.pos.model.WomanShopSalesIOManager;

public class MainMenuActivity extends Activity implements DataSyncTaskCallback {

	private WomanShopIOManager womanShopIOManager;
	private DatabaseHelper databaseHelper;
	private SalesDatabaseHelper salesDatabaseHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);

		womanShopIOManager = new WomanShopIOManager();
		databaseHelper = new DatabaseHelper(this);
		womanShopIOManager.setDatabase(databaseHelper.getWritableDatabase());

		salesDatabaseHelper = new SalesDatabaseHelper(this);
		WomanShopSalesIOManager.getInstance().setDatabase(salesDatabaseHelper.getWritableDatabase());

		findViewById(R.id.RegisterButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent().setClass(
						MainMenuActivity.this,
						CategoryListActivity.class);
				startActivity(intent);
			}
		});

		findViewById(R.id.EditButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent().setClass(
						MainMenuActivity.this,
						EditActivity.class);
				startActivity(intent);
			}
		});

		setRegisterButtonAndEditButtonEnabled();

		findViewById(R.id.SalesButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent().setClass(MainMenuActivity.this, SalesCalenderActivity.class);
				startActivity(intent);
			}
		});

		findViewById(R.id.SyncButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				DataSyncTask syncTask = new DataSyncTask(
						MainMenuActivity.this,
						MainMenuActivity.this,
						womanShopIOManager);
				syncTask.execute();
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		womanShopIOManager.closeDatabase();
		WomanShopSalesIOManager.getInstance().closeDatabase();
		databaseHelper.close();
		salesDatabaseHelper.close();
		WomanShopSalesIOManager.removeInstance();
		Log.d("debug", "Exit onDestroy");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_library:
				Intent intent = new Intent().setClass(MainMenuActivity.this, LicenseActivity.class);
				startActivity(intent);
				break;
			default:
				return super.onOptionsItemSelected(item);
		}

		return true;
	}

	@Override
	public void onSuccessSyncData() {
		Toast.makeText(this, R.string.sync_success, Toast.LENGTH_LONG).show();
		setRegisterButtonAndEditButtonEnabled();
	}

	@Override
	public void onFailedSyncData(int resId) {
		Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
		setRegisterButtonAndEditButtonEnabled();
	}

	private void setRegisterButtonAndEditButtonEnabled() {
		if (ProductsManager.getInstance().getCategoryCount() > 0) {
			findViewById(R.id.RegisterButton).setEnabled(true);
			findViewById(R.id.EditButton).setEnabled(true);
		} else {
			findViewById(R.id.RegisterButton).setEnabled(false);
			findViewById(R.id.EditButton).setEnabled(false);
		}
	}
}
