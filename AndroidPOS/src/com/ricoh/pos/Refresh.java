package com.ricoh.pos;

import android.support.v4.app.FragmentManager;

public interface Refresh {
	void refresh(FragmentManager fragmentManager,String latestItemId,String latestSearchWord);
}
