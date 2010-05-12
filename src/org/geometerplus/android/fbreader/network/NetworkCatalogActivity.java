/*
 * Copyright (C) 2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.android.fbreader.network;

import java.util.*;

import android.app.Activity;
import android.app.ListActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.IBinder;
import android.view.*;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.BaseAdapter;
import android.net.Uri;
import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Bitmap;

import org.geometerplus.zlibrary.ui.android.R;

import org.geometerplus.zlibrary.core.tree.ZLTree;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.image.ZLImage;

import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageManager;
import org.geometerplus.zlibrary.ui.android.image.ZLAndroidImageData;
import org.geometerplus.zlibrary.ui.android.dialogs.ZLAndroidDialogManager;

import org.geometerplus.fbreader.network.*;
import org.geometerplus.fbreader.network.tree.*;
import org.geometerplus.fbreader.network.authentication.*;


public class NetworkCatalogActivity extends NetworkBaseActivity {

	public static final String CATALOG_LEVEL_KEY = "org.geometerplus.android.fbreader.network.CatalogLevel";
	public static final String CATALOG_KEY_KEY = "org.geometerplus.android.fbreader.network.CatalogKey";

	private NetworkTree myTree;
	private String myCatalogKey;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		final NetworkView networkView = NetworkView.Instance();
		if (!networkView.isInitialized()) {
			finish();
			return;
		}

		final Intent intent = getIntent();
		final int level = intent.getIntExtra(CATALOG_LEVEL_KEY, -1);
		if (level == -1) {
			throw new RuntimeException("Catalog's Level was not specified!!!");
		}

		myCatalogKey = intent.getStringExtra(CATALOG_KEY_KEY);
		if (myCatalogKey == null) {
			throw new RuntimeException("Catalog's Key was not specified!!!");
		}

		myTree = networkView.getOpenedTree(level);
		if (myTree == null) {
			finish();
			return;
		}

		networkView.setOpenedActivity(myCatalogKey, this);

		setListAdapter(new CatalogAdapter());
		getListView().invalidateViews();
		setupTitle();
	}

	private final void setupTitle() {
		String title = null;
		final NetworkView networkView = NetworkView.Instance();
		if (networkView.isInitialized()) {
			final NetworkTreeActions actions = networkView.getActions(myTree);
			if (actions != null) {
				title = actions.getTreeTitle(myTree);
			}
		}
		if (title == null) {
			title = myTree.getName();
		}
		setTitle(title);

		boolean inProgress = false;
		final String key = getNetworkTreeKey(myTree, true);
		if (key != null && networkView.isInitialized() && networkView.containsItemsLoadingRunnable(key)) {
			inProgress = true;
		}
		setProgressBarIndeterminateVisibility(inProgress);
	}

	private static String getNetworkTreeKey(NetworkTree tree, boolean recursive) {
		if (tree instanceof NetworkCatalogTree) {
			return ((NetworkCatalogTree) tree).Item.URLByType.get(NetworkCatalogItem.URL_CATALOG);
		} else if (tree instanceof SearchItemTree) {
			return NetworkSearchActivity.SEARCH_RUNNABLE_KEY;
		} else if (recursive && tree.Parent instanceof NetworkTree) {
			if (tree instanceof NetworkAuthorTree
					|| tree instanceof NetworkSeriesTree) {
				return getNetworkTreeKey((NetworkTree) tree.Parent, true);
			}
		}
		return null;
	}


	@Override
	public void onDestroy() {
		if (myTree != null && myCatalogKey != null && NetworkView.Instance().isInitialized()) {
			NetworkView.Instance().setOpenedActivity(myCatalogKey, null);
		}
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
	}


	private final class CatalogAdapter extends BaseAdapter {

		private ArrayList<NetworkTree> mySpecialItems;

		public CatalogAdapter() {
			mySpecialItems = null;
			if (myTree instanceof NetworkCatalogRootTree) {
				NetworkCatalogTree tree = (NetworkCatalogTree) myTree;
				NetworkAuthenticationManager mgr = tree.Item.Link.authenticationManager();
				if (mgr != null) {
					mySpecialItems = new ArrayList<NetworkTree>();
					if (mgr.refillAccountSupported()) {
						mySpecialItems.add(new RefillAccountTree(tree));
					}
					mySpecialItems.trimToSize();
				}
			}
		}

		public final int getCount() {
			return myTree.subTrees().size() +
				((mySpecialItems == null) ? 0 : mySpecialItems.size());
		}

		public final NetworkTree getItem(int position) {
			if (position < 0) {
				return null;
			}
			if (position < myTree.subTrees().size()) {
				return (NetworkTree) myTree.subTrees().get(position);
			}
			position -= myTree.subTrees().size();
			if (mySpecialItems != null && position < mySpecialItems.size()) {
				return mySpecialItems.get(position);
			}
			return null;
		}

		public final long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, final ViewGroup parent) {
			final NetworkTree tree = getItem(position);
			return setupNetworkTreeItemView(convertView, parent, tree);
		}
	}

	@Override
	public void onModelChanged() {
		getListView().invalidateViews();
		setupTitle();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			doStopLoading();
		}
		return super.onKeyDown(keyCode, event);
	}

	private void doStopLoading() {
		final String key = getNetworkTreeKey(myTree, false);
		if (key != null && NetworkView.Instance().isInitialized()) {
			final ItemsLoadingRunnable runnable = NetworkView.Instance().getItemsLoadingRunnable(key);
			if (runnable != null) {
				runnable.interrupt();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		return NetworkView.Instance().createOptionsMenu(menu, myTree);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return NetworkView.Instance().prepareOptionsMenu(menu, myTree);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (NetworkView.Instance().runOptionsMenu(this, item, myTree)) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}