package com.sprd.cameracalibration;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import android.util.Log;

import com.sprd.cameracalibration.R;
import com.sprd.cameracalibration.modules.TestItem;
import com.sprd.cameracalibration.modules.UnitTestItemList;
import com.sprd.cameracalibration.utils.CameraUtil;
import com.sprd.cameracalibration.utils.SharedPreferencesUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class ListItemTestActivity extends Activity {
    private static final String TAG = "ListItemTestActivity";
    public static final String INTENT_ACTION_CAMERACALIBRATION = "com.sprd.cameracalibration.START_CAMERACALIBRATION";
    private ItemListViewAdapter mItemListViewAdapter;
    private ArrayList<TestItem> mItemsListView = new ArrayList<TestItem>();
    private HashMap<String,String> mPhasecheckHashMap = new HashMap<>();
    private ListView mListViewItem;
    private int mLastTestItemIndex = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validation_tools_main);
        mListViewItem = (ListView) findViewById(R.id.ValidationToolsList);

        Intent intent = getIntent();
        String action = intent.getAction();
        Log.d(TAG,"onCreate intent action = " + action);
        if (INTENT_ACTION_CAMERACALIBRATION.equals(action)){
            String phasecheckDataString = intent.getStringExtra("phasecheck_result");
            if (phasecheckDataString != null){
                String[] phasecheckData = phasecheckDataString.split("\n");
                for (String item : phasecheckData){
                    String[] phasecheckItem = item.split(":");
                    Log.i(TAG,phasecheckItem[0] + " : " + phasecheckItem[1]);
                    mPhasecheckHashMap.put(phasecheckItem[0],phasecheckItem[1]);
                }
            }
        }

        SharedPreferencesUtil.init(ListItemTestActivity.this);
        CameraUtil.initFeatureList(ListItemTestActivity.this);
        initAdapter();
        mListViewItem.setAdapter(mItemListViewAdapter);
        mListViewItem.setOnItemClickListener(new ListItemClickListener());
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    }

    @Override
    protected void onDestroy() {
        mItemsListView.clear();
        mPhasecheckHashMap.clear();
        super.onDestroy();
    }

    private void initAdapter() {
        mItemsListView.addAll(UnitTestItemList.getInstance(
                ListItemTestActivity.this).getTestItemList());

        for (TestItem item : mItemsListView) {
            Log.d(TAG,"item className = " + item.getTestClassName());
            String phasecheckName = Const.getPhasecheckName(item.getTestClassName());
            Log.d(TAG,"phasecheckName = " + phasecheckName);
            if (phasecheckName != null){
                String phasecheckResult = mPhasecheckHashMap.get(phasecheckName);
                Log.d(TAG,"phasecheckResult = " + phasecheckResult);
                if (phasecheckResult != null && phasecheckResult.equals("PASS")){
                    item.setTestResult(Const.SUCCESS);
                }
                if (phasecheckResult != null && phasecheckResult.equals("FAIL")){
                    item.setTestResult(Const.FAIL);
                }
                if (phasecheckResult != null && phasecheckResult.equals("UnTested")){
                    item.setTestResult(Const.DEFAULT);
                }
            }
        }
        mItemListViewAdapter = new ItemListViewAdapter(this, mItemsListView);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Const.TEST_ITEM_DONE) {
            int position = mLastTestItemIndex;
            mItemsListView.get(position).setTestResult(intent.getIntExtra("isSuccess",Const.DEFAULT));
            mItemListViewAdapter.notifyDataSetChanged();
        }
    }

    private class ListItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {

            mLastTestItemIndex = position;
            Intent intent = new Intent();
            intent.setClassName(ListItemTestActivity.this,
                    mItemsListView.get(position).getTestClassName());
            intent.putExtra(Const.INTENT_PARA_TEST_NAME,
                    mItemsListView.get(position).getTestName());

            intent.putExtra(Const.INTENT_PARA_TEST_INDEX, position);
            startActivityForResult(intent, 0);
        }
    }

    private class ItemListViewAdapter extends BaseAdapter {

        private ArrayList<TestItem> mItemList;
        private LayoutInflater mInflater;

        public ItemListViewAdapter(Context c, ArrayList<TestItem> mItemsListView) {
            mItemList = mItemsListView;
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            if (mItemList != null) {
                return mItemList.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            TestItem item = mItemList.get(position);
            if (convertView == null) {
                view = mInflater.inflate(R.layout.listview_item, parent, false);
            } else {
                view = convertView;
            }
            TextView textView = (TextView) view.findViewById(R.id.listitem_text);
            textView.setText(item.getTestName());

            if (item.getTestResult() == Const.SUCCESS) {
                textView.setTextColor(Color.GREEN);
            } else if (item.getTestResult() == Const.FAIL) {
                textView.setTextColor(Color.RED);
            } else {
                textView.setTextColor(Color.WHITE);
            }
            return view;
        }
    }
}
