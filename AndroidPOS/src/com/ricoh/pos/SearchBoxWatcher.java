package com.ricoh.pos;

import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

public class SearchBoxWatcher implements TextWatcher {

    private String latestItemId;
    private String latestSearchWord;
    private FragmentManager fragmentManager;
    private Refresh refresh;

    SearchBoxWatcher(String latestItemId, String latestSearchWord, FragmentManager fragmentManager, Refresh refresh) {
        this.latestItemId = latestItemId;
        this.latestSearchWord = latestSearchWord;
        this.fragmentManager = fragmentManager;
        this.refresh = refresh;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        Log.d("beforeTextChanged", "beforeTextChanged");
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        latestSearchWord = s.toString();
        refresh.refresh(fragmentManager, latestItemId, latestSearchWord);
    }

    @Override
    public void afterTextChanged(Editable s) {
        Log.d("afterTextChanged", "afterTextChanged");
    }
}
