/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.omnirom.omnigreen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class RunningProcessesView extends FrameLayout implements
        AdapterView.OnItemClickListener, RecyclerListener {

    final int mMyUserId;

    final HashMap<View, ActiveItem> mActiveItems = new HashMap<View, ActiveItem>();

    ActivityManager mAm;

    RunningState mState;

    StringBuilder mBuilder = new StringBuilder(128);

    RunningState.BaseItem mCurSelected;

    ListView mListView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    ServiceListAdapter mAdapter;

    Dialog mCurDialog;

    public static class ActiveItem {
        View mRootView;
        RunningState.BaseItem mItem;
        ActivityManager.RunningServiceInfo mService;
        ViewHolder mHolder;
        long mFirstRunTime;
        boolean mSetBackground;
    }

    public static class ViewHolder {
        public View rootView;
        public ImageView icon;
        public TextView name;
        public TextView description;

        public ViewHolder(View v) {
            rootView = v;
            icon = (ImageView) v.findViewById(R.id.icon);
            name = (TextView) v.findViewById(R.id.name);
            description = (TextView) v.findViewById(R.id.description);
            v.setTag(this);
        }

        public ActiveItem bind(RunningState state, RunningState.BaseItem item,
                StringBuilder builder) {
            PackageManager pm = rootView.getContext().getPackageManager();
            if (item.mPackageInfo == null
                    && item instanceof RunningState.MergedItem) {
                // Items for background processes don't normally load
                // their labels for performance reasons. Do it now.
                RunningState.MergedItem mergedItem = (RunningState.MergedItem) item;
                if (mergedItem.mProcess != null) {
                    ((RunningState.MergedItem) item).mProcess.ensureLabel(pm);
                    item.mPackageInfo = ((RunningState.MergedItem) item).mProcess.mPackageInfo;
                    item.mDisplayLabel = ((RunningState.MergedItem) item).mProcess.mDisplayLabel;
                }
            }
            name.setText(item.mDisplayLabel);
            ActiveItem ai = new ActiveItem();
            ai.mRootView = rootView;
            ai.mItem = item;
            ai.mHolder = this;
            ai.mFirstRunTime = item.mActiveSince;
            if (item.mBackground) {
                description.setText(rootView.getContext().getText(
                        R.string.cached));
            } else {
                description.setText(item.mDescription);
            }
            item.mCurSizeStr = null;
            icon.setImageDrawable(item.loadIcon(rootView.getContext(), state));
            icon.setVisibility(View.VISIBLE);
            return ai;
        }
    }

    static class TimeTicker extends TextView {
        public TimeTicker(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }

    class ServiceListAdapter extends BaseAdapter {
        final RunningState mState;
        final LayoutInflater mInflater;
        boolean mShowBackground;
        ArrayList<RunningState.MergedItem> mOrigItems;
        final ArrayList<RunningState.MergedItem> mItems = new ArrayList<RunningState.MergedItem>();

        ServiceListAdapter(RunningState state) {
            mState = state;
            mInflater = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            refreshItems();
        }

        void setShowBackground(boolean showBackground) {
            if (mShowBackground != showBackground) {
                mShowBackground = showBackground;
                mState.setWatchingBackgroundItems(showBackground);
                refreshItems();
                refreshUi(true);
            }
        }

        boolean getShowBackground() {
            return mShowBackground;
        }

        void refreshItems() {
            ArrayList<RunningState.MergedItem> newItems = mShowBackground ? mState
                    .getCurrentBackgroundItems() : mState
                    .getCurrentMergedItems();
            if (mOrigItems != newItems) {
                mOrigItems = newItems;
                if (newItems == null) {
                    mItems.clear();
                } else {
                    mItems.clear();
                    mItems.addAll(newItems);
                    if (mShowBackground) {
                        Collections.sort(mItems, mState.mBackgroundComparator);
                    }
                }
            }
        }

        public boolean hasStableIds() {
            return true;
        }

        public int getCount() {
            return mItems.size();
        }

        @Override
        public boolean isEmpty() {
            return mItems.size() == 0;
        }

        public Object getItem(int position) {
            return mItems.get(position);
        }

        public long getItemId(int position) {
            return mItems.get(position).hashCode();
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return !mItems.get(position).mIsProcess;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }

        public View newView(ViewGroup parent) {
            View v = mInflater.inflate(R.layout.running_processes_item, parent,
                    false);
            new ViewHolder(v);
            return v;
        }

        public void bindView(View view, int position) {
            if (position >= mItems.size()) {
                // List must have changed since we last reported its
                // size... ignore here, we will be doing a data changed
                // to refresh the entire list.
                return;
            }
            ViewHolder vh = (ViewHolder) view.getTag();
            RunningState.MergedItem item = mItems.get(position);
            ActiveItem ai = vh.bind(mState, item, mBuilder);
            mActiveItems.put(view, ai);
        }
    }

    void refreshUi(boolean dataChanged) {
        if (dataChanged) {
            ServiceListAdapter adapter = mAdapter;
            adapter.refreshItems();
            adapter.notifyDataSetChanged();
        }
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        ListView l = (ListView) parent;
        RunningState.MergedItem mi = (RunningState.MergedItem) l.getAdapter()
                .getItem(position);
        mCurSelected = mi;
    }

    public void onMovedToScrapHeap(View view) {
        mActiveItems.remove(view);
    }

    public RunningProcessesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMyUserId = UserHandle.myUserId();
    }

    public void doCreate(Bundle savedInstanceState) {
        mAm = (ActivityManager) getContext().getSystemService(
                Context.ACTIVITY_SERVICE);
        mState = RunningState.getInstance(getContext());
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.running_processes_view, this);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mListView = (ListView) findViewById(android.R.id.list);
        View emptyView = findViewById(com.android.internal.R.id.empty);
        if (emptyView != null) {
            mListView.setEmptyView(emptyView);
        }
        mListView.setOnItemClickListener(this);
        mListView.setRecyclerListener(this);
        mAdapter = new ServiceListAdapter(mState);
        mListView.setAdapter(mAdapter);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                update();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    void update() {
        mState.updateNow();
        refreshUi(true);
    }
}
