package com.ricoh.pos;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.EditText;

public class CategoryListActivity extends FragmentActivity implements
        CategoryListFragment.Callbacks, TotalPaymentFragment.OnOkButtonClickListener, Refresh {

    private boolean mTwoPane;
    private String latestItemId;
    private String latestSearchWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_list);
        latestItemId = getString(R.string.category_title_default);
        if (findViewById(R.id.category_detail_container) != null) {

            mTwoPane = true;
            ((CategoryListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.category_list))
                    .setActivateOnItemClick(true);

            Bundle arguments = new Bundle();
            arguments.putString(CategoryDetailFragment.ARG_ITEM_ID, getString(R.string.category_title_default));
            final CategoryDetailFragment fragment = new CategoryDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.category_detail_container, fragment).commit();

            TotalPaymentFragment paymentFragment = new TotalPaymentFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.total_payment_container, paymentFragment).commit();

            final EditText editText = (EditText) findViewById(R.id.id_search_text_box);

            editText.addTextChangedListener(new SearchBoxWatcher(latestItemId, latestSearchWord, getSupportFragmentManager(), this));
        }
    }

    @Override
    public void onItemSelected(String id) {
        latestItemId = id;
        if (mTwoPane) {
            Bundle arguments = new Bundle();

            final EditText editText = (EditText) findViewById(R.id.id_search_text_box);
            latestSearchWord = editText.getText().toString();

            arguments.putString(CategoryDetailFragment.ARG_ITEM_ID, id);
            arguments.putString(CategoryDetailFragment.ARG_SEARCH_WORD, latestSearchWord);
            CategoryDetailFragment fragment = new CategoryDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.category_detail_container, fragment).commit();
            editText.addTextChangedListener(new SearchBoxWatcher(latestItemId, latestSearchWord, getSupportFragmentManager(), this));
        } else {
            Intent detailIntent = new Intent(this, CategoryDetailActivity.class);
            detailIntent.putExtra(CategoryDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

    @Override
    public void onOkClicked() {
        startActivity(new Intent(this, RegisterConfirmActivity.class));
    }

    @Override
    public void refresh(FragmentManager fragmentManager, String latestItemId, String latestSearchWord) {
        CategoryDetailFragment fragment = new CategoryDetailFragment();
        Bundle arguments = new Bundle();
        arguments.putString(CategoryDetailFragment.ARG_ITEM_ID, latestItemId);
        arguments.putString(CategoryDetailFragment.ARG_SEARCH_WORD, latestSearchWord);
        fragment.setArguments(arguments);
        fragmentManager.beginTransaction()
                .replace(R.id.category_detail_container, fragment).commit();
    }
}
