package com.wl.android.filltool.fragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wl.android.filltool.R;

import java.util.ArrayList;

import static android.R.attr.duration;
import static android.R.attr.type;

/**
 * Created by D22397 on 2017/9/4.
 * 通话记录填充
 */

public class CallLogFillFragment extends Fragment {
    private static final String TAG = "CallLogFillFragment";
    private static final String CallLog_URI = "content://call_log/calls";

    private static String[] sTelFirst = ("134,135,136,137,138,139,150,151,152,157" +
            ",158,159,130,131,132,155,156,133,153").split(",");

    Button btn1;
    Button btn2;
    private TextView mCalllogNumTextView;
    private EditText mAddcalllogNumEditText;
    private ProgressDialog mProgressDialog;

    private boolean mIsAdd = false;
    private int num = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_call_log_fill, container, false);

        mCalllogNumTextView = (TextView) v.findViewById(R.id.CallLog_num_text_view);
        mAddcalllogNumEditText = (EditText) v.findViewById(R.id.add_calllognum_edit_text);
        //定义填充进度条
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle("通话记录填充");
        mProgressDialog.setMessage("填充中，请稍候....");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        btn1 = (Button) v.findViewById(R.id.AddCallLog);
        btn2 = (Button) v.findViewById(R.id.DeleteCallLog);

        updateNumCallLog();

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mEdit = mAddcalllogNumEditText.getText().toString();
                if (!TextUtils.isEmpty(mEdit)) {
                    num = Integer.parseInt(mEdit);
                    if (!mIsAdd) {
                        final AddCalllogTask addCalllogTask = new AddCalllogTask();
                        addCalllogTask.execute(num);
                    }
                } else {
                    Toast.makeText(getActivity(), "Please input a number！", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteCallLog();
            }
        });

        return v;
    }


    //添加通话记录
    private void insertCallLog() {
        Uri uri = Uri.parse("content://call_log/calls");
        int num;
        ArrayList<String> telList = new ArrayList<>();  //存贮电话号码的数组
        String edit = mAddcalllogNumEditText.getText().toString();   //获取输入框的数据
        if (!TextUtils.isEmpty(edit)) {    //判断输入框内容是否为空
            num = Integer.parseInt(edit);   //将输入框中的数据转换成Int型
            for (int i = 0; i < num; i++) {
                telList.add(getTel());
            }
            Log.d(TAG, telList.toString());
            //循环插入ID
            ArrayList<Long> idLists = new ArrayList<>();
            for (int i = 0; i < telList.size(); i++) {
                ContentValues values = new ContentValues();
                ContentResolver resolver = getActivity().getContentResolver();
                long id = ContentUris.parseId(resolver.insert(uri, values));
                idLists.add(id);
            }
            //向链表中插入通话记录
            for (int j = 0; j < idLists.size(); j++) {
                ContentValues values = new ContentValues();
                values.put(CallLog.Calls.NUMBER, telList.get(j));
                values.put(CallLog.Calls.DATE, System.currentTimeMillis());
                values.put(CallLog.Calls.DURATION, duration);
                values.put(CallLog.Calls.TYPE, type);
                String isNew = null;
                values.put(CallLog.Calls.NEW, isNew);

            }
            Toast.makeText(getActivity(), "Insert" + num + " Successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    //删除通话记录
    private void deleteCallLog() {
        ContentResolver resolver = getActivity().getContentResolver();
        Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI,
                null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            resolver.delete(CallLog.Calls.CONTENT_URI, null, null);
            break;
        }
        cursor.close();
        Toast.makeText(getActivity(), "Delete  Successfully!", Toast.LENGTH_SHORT).show();
        updateNumCallLog();
        if (ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_CALL_LOG}, 1000);
        }
    }

    //获得电话号码
    private String getTel() {
        int index = getNum(0, sTelFirst.length - 1);
        String first = sTelFirst[index];
        String second = String.valueOf(getNum(1, 888) + 10000).substring(1);
        String third = String.valueOf(getNum(1, 9100) + 10000).substring(1);
        return first + second + third;
    }

    public int getNum(int start, int end) {
        return (int) (Math.random() * (end - start + 1) + start);
    }

    private void updateNumCallLog() {
        mCalllogNumTextView.setText("通话记录条数：" + getCallLogNum());
    }

    private int getCallLogNum() {
        int count = 0;
        Uri uri = Uri.parse(CallLog_URI);
        Cursor cursor = null;
        try {
            cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                count = cursor.getCount();
                Log.d(TAG, "count:" + count);
            }
        } catch (SQLiteException e) {
            Log.d(TAG, e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    class AddCalllogTask extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.setMax(num);
            mProgressDialog.show();
            mIsAdd = true;
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            int num = params[0];
            ContentResolver resolver = getActivity().getContentResolver();
            Uri uri = Uri.parse("content://call_log/calls");
            ArrayList<String> telList = new ArrayList<>();  //存贮电话的数组
            for (int i = 0; i < num; i++) {
                String tel = getTel();
                boolean isContains = true;
                while (isContains) {
                    if (!telList.contains(tel)) {
                        telList.add(tel);
                        isContains = false;
                    } else {
                        tel = getTel();
                        isContains = true;
                    }
                }
            }
            //Log.d(TAG, telList.toString());
            //向链表中插入通话记录
            for (int j = 0; j < telList.size(); j++) {
                ContentValues values = new ContentValues();
                values.put(CallLog.Calls.NUMBER, telList.get(j));
                values.put(CallLog.Calls.DATE, System.currentTimeMillis());
                values.put(CallLog.Calls.DURATION, duration);
                values.put(CallLog.Calls.TYPE, type);
                String isNew = null;
                values.put(CallLog.Calls.NEW, isNew);
                Uri uri2 = resolver.insert(uri, values);
                if (uri2 == null) {
                    return 0;
                }
                publishProgress(j + 1);
            }
            return 1;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            mProgressDialog.dismiss();
            mIsAdd = false;
            switch (integer) {
                case 0:
                    Toast.makeText(getActivity(), "Insert Failed!", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    updateNumCallLog();
                    Toast.makeText(getActivity(), "Insert Successfully!", Toast.LENGTH_SHORT).show();
                    mProgressDialog.incrementProgressBy(-mProgressDialog.getProgress());
                    break;
                default:
                    break;
            }
        }

    }
}
