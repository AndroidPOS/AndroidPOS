
package com.ricoh.pos;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.widget.EditText;


public class EditActivity extends FragmentActivity implements
        CategoryListFragment.Callbacks, Refresh {

    private String latestItemId;
    private String latestSearchWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        latestItemId = getString(R.string.category_title_default);

        ((CategoryListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.category_list_2))
                .setActivateOnItemClick(true);

        Bundle arguments = new Bundle();
        arguments.putString(ProductFragment.ARG_ITEM_ID, getString(R.string.category_title_default));
        final ProductFragment fragment = new ProductFragment();
        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.product_list, fragment).commit();

        final EditText editText = (EditText) findViewById(R.id.id_search_text_box_2);
        editText.addTextChangedListener(new SearchBoxWatcher(latestItemId, latestSearchWord, getSupportFragmentManager(), this));
    }

    @Override
    public void onItemSelected(String id) {
        latestItemId = id;
        Bundle arguments = new Bundle();

        final EditText editText = (EditText) findViewById(R.id.id_search_text_box_2);
        latestSearchWord = editText.getText().toString();

        arguments.putString(ProductFragment.ARG_ITEM_ID, id);
        arguments.putString(ProductFragment.ARG_SEARCH_WORD, latestSearchWord);
        ProductFragment fragment = new ProductFragment();
        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.product_list, fragment).commit();

        editText.addTextChangedListener(new SearchBoxWatcher(latestItemId, latestSearchWord, getSupportFragmentManager(), this));
    }

    @Override
    public void refresh(FragmentManager fragmentManager, String latestItemId, String latestSearchWord) {
        ProductFragment fragment = new ProductFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ProductFragment.ARG_ITEM_ID, latestItemId);
        arguments.putString(ProductFragment.ARG_SEARCH_WORD, latestSearchWord);

        fragment.setArguments(arguments);
        fragmentManager.beginTransaction()
                .replace(R.id.product_list, fragment).commit();
    }
}
