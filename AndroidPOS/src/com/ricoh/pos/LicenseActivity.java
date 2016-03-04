package com.ricoh.pos;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LicenseActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_license);

		BufferedReader br = null;
		String text = "";

		try {
			br = new BufferedReader(new InputStreamReader(getAssets().open("license/apache_license_2.0.txt")));
			String str;
			while ((str = br.readLine()) != null) {
				text += str + "\n";
			}
		} catch (IOException e) {
			text = getString(R.string.failed_loading_the_license_text);
		} finally {
			IOUtils.closeQuietly(br);
		}

		((TextView) findViewById(R.id.commons_io_license)).setText(text);
	}
}
