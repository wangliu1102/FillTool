package com.wl.android.filltool;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class FillSmsActivity extends AppCompatActivity {
    private static final String TAG = "FillSmsActivity";
    private static final String SMS_URI_INBOX = "content://sms/inbox"; // 收件箱
    private static String[] sTelFirst = ("134,135,136,137,138,139,150,151,152,157" +
            ",158,159,130,131,132,155,156,133,153").split(",");

    private String defaultSmsPkg;
    private String mySmsPkg;
    private EditText mAddNumEditText;
    private TextView mMessageNumTextView;
    private Button mAddMessageButton;
    private Button mDeleteAllMessageButton;
    private ProgressDialog mProgressDialog;// 填充短信时的进度框
    private ProgressDialog mProgressDialog2;// 删除短信时的进度框

    private AddMessageTask addMessageTask = null;
    private ArrayList<String> mSmsTelList = new ArrayList<>();
    private boolean mIsDelete = false;
    private boolean mIsAdd = false;
    private int num = 0;

    PowerManager powerManager = null;
    PowerManager.WakeLock wakeLock = null;


    /**
     * 异步删除所有短信的Handler
     */
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            mProgressDialog2.dismiss();
            mIsDelete = false;
            int d = msg.what;
            if (d != 0) {
                mSmsTelList.clear();
                updateNumMessage();
                Toast.makeText(FillSmsActivity.this, "成功删除所有短信.", Toast.LENGTH_LONG).show();
                RestoreDefaultApp();
            } else {
                Toast.makeText(FillSmsActivity.this, "当前收件箱已填充短信为0条.", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_sms_fill);
        powerManager = (PowerManager) this.getSystemService(this.POWER_SERVICE);
        wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");

        SharedPreferences preferDataList = getSharedPreferences("SmsList", MODE_PRIVATE);
        int environNums = preferDataList.getInt("SmsNums", 0);
        for (int i = 0; i < environNums; i++) {
            mSmsTelList.add(preferDataList.getString("item_" + i, null));
        }
        Log.d(TAG, "mSmsTelList: " + mSmsTelList.size());

        mAddNumEditText = (EditText) findViewById(R.id.add_num_edit_text);
        mMessageNumTextView = (TextView) findViewById(R.id.message_num_text_view);
        mAddMessageButton = (Button) findViewById(R.id.add_message_button);
        mDeleteAllMessageButton = (Button) findViewById(R.id.deleteAll_message_button);

        defaultSmsPkg = Telephony.Sms.getDefaultSmsPackage(this);
        Log.d(TAG, "onCreate: " + defaultSmsPkg);
        mySmsPkg = this.getPackageName();

        if (!defaultSmsPkg.equals(mySmsPkg)) {
//            如果这个App不是默认的Sms App，则修改成默认的SMS APP
//            因为从Android 4.4开始，只有默认的SMS APP才能对SMS数据库进行处理
            updateDefaultApp();
        }


        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FillSmsActivity.this,
                    new String[]{Manifest.permission.READ_SMS}, 1);
        } else {
            updateNumMessage();
        }

        mProgressDialog = new ProgressDialog(FillSmsActivity.this);
        mProgressDialog.setTitle("填充数据");
        mProgressDialog.setMessage("正在填充短信，请稍后...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Stop",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        addMessageTask.cancel(true);
                        addMessageTask = null;
                    }
                });

        mAddMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mySmsPkg.equals(Telephony.Sms.getDefaultSmsPackage(FillSmsActivity.this))) {
                    Log.i(TAG, "My App is default SMS App.");
                    String edit = mAddNumEditText.getText().toString();
                    if (!TextUtils.isEmpty(edit)) {
                        if (edit.startsWith("0")) {
                            Toast.makeText(FillSmsActivity.this, "输入不能以 0 开头！", Toast.LENGTH_SHORT).show();
                        } else {
                            num = Integer.parseInt(edit);
                            if (!mIsAdd) {
                                addMessageTask = new AddMessageTask();
                                addMessageTask.execute(num);
                            }
                        }
                    } else {
                        Toast.makeText(FillSmsActivity.this, "请输入要填充的短信条数！",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(FillSmsActivity.this, "Sorry,the App is not default Sms App.",
                            Toast.LENGTH_LONG).show();
                    updateDefaultApp();
                }
//                addMessage();

            }
        });

        mDeleteAllMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mySmsPkg.equals(Telephony.Sms.getDefaultSmsPackage(FillSmsActivity.this))) {
                    Log.i(TAG, "My App is default SMS App.");
                    if (!mIsDelete) {
                        deleteAllMessage();
                        mIsDelete = true;
                    }

                } else {
                    Toast.makeText(FillSmsActivity.this, "Sorry,the App is not default Sms App.",
                            Toast.LENGTH_LONG).show();
                    updateDefaultApp();
                }
            }
        });
    }

    /**
     * 处理权限回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateNumMessage();
                } else {
                    Toast.makeText(FillSmsActivity.this, "You denied the permission",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        wakeLock.acquire();
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FillSmsActivity.this,
                    new String[]{Manifest.permission.READ_SMS}, 1);
        } else {
            updateNumMessage();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        wakeLock.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        // 保存未销毁状态下的所有电话号码，主要是为了防止下次生成短信时电话号码重复的问题
        SharedPreferences.Editor editor = getSharedPreferences("SmsList", MODE_PRIVATE).edit();
        editor.putInt("SmsNums", mSmsTelList.size());
        for (int i = 0; i < mSmsTelList.size(); i++) {
            editor.putString("item_" + i, mSmsTelList.get(i));
        }
        editor.apply();
    }

    /**
     * 监听按键
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { // 监听返回按钮
            moveTaskToBack(false); // 设为false，点击返回键回到桌面不会销毁本次的Activity
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 删除短信
     */
    private void deleteAllMessage() {
        mProgressDialog2 = ProgressDialog.show(FillSmsActivity.this, "删除数据", "正在删除短信，请稍后...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                ContentResolver resolver = getContentResolver();
                int d = resolver.delete(Telephony.Sms.CONTENT_URI, Telephony.Sms.PERSON + "= ?",
                        new String[]{"1"});
                ContentValues values = new ContentValues();
                values.put(Telephony.Sms.PERSON, "22");
                resolver.insert(Telephony.Sms.CONTENT_URI, values);
                resolver.delete(Telephony.Sms.CONTENT_URI,Telephony.Sms.PERSON + "= ?",
                        new String[]{"22"});
                Message msg = new Message();
                msg.what = d;
                mHandler.sendMessageDelayed(msg, 1000);
            }
        }).start();


    }


    private void addMessage() {
        if (mySmsPkg.equals(Telephony.Sms.getDefaultSmsPackage(FillSmsActivity.this))) {

            Log.i(TAG, "My App is default SMS App.");

            ContentResolver resolver = getContentResolver();

            int num = 0;
            String edit = mAddNumEditText.getText().toString();
            if (!TextUtils.isEmpty(edit)) {
                num = Integer.parseInt(edit);
            }
            String tel = null;
            ArrayList<String> telList = new ArrayList<>();
            boolean isContains = true;
            for (int i = 0; i < num; i++) {
                tel = getTel();
                isContains = true;
                while (isContains) {
                    if (!mSmsTelList.contains(tel)) {
                        telList.add(tel);
                        mSmsTelList.add(tel);
                        isContains = false;
                    } else {
                        tel = getTel();
                        isContains = true;
                    }
                }
            }

            Log.d(TAG, telList.toString());
            for (String phone : telList) {
                ContentValues values = new ContentValues();
                values.put(Telephony.Sms.ADDRESS, phone);
                values.put(Telephony.Sms.PERSON, "1");
                values.put(Telephony.Sms.DATE, System.currentTimeMillis());
                long dateSent = System.currentTimeMillis() - 5000;
                values.put(Telephony.Sms.DATE_SENT, dateSent);
                values.put(Telephony.Sms.READ, false);
                values.put(Telephony.Sms.SEEN, false);
                values.put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE);
                values.put(Telephony.Sms.BODY, "这是" + phone + "的短信！！！");
                values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);

                Uri uri = resolver.insert(Telephony.Sms.CONTENT_URI, values);
                if (uri != null) {
                    long uriId = ContentUris.parseId(uri);
                }
            }

            updateNumMessage();
            Toast.makeText(FillSmsActivity.this, "Insert " + num + "Messages.", Toast.LENGTH_LONG).show();

            RestoreDefaultApp();

        } else {
            Toast.makeText(FillSmsActivity.this, "Sorry,the App is not default Sms App.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 随机生成一定范围的整数
     *
     * @param start
     * @param end
     * @return
     */
    public int getNum(int start, int end) {
        return (int) (Math.random() * (end - start + 1) + start);
    }

    /**
     * 返回手机号码
     */
    private String getTel() {
        int index = getNum(0, sTelFirst.length - 1);
        String first = sTelFirst[index];
        String second = String.valueOf(getNum(1, 9999) + 10000).substring(1);
        String third = String.valueOf(getNum(1, 9999) + 10000).substring(1);
        return first + second + third;
    }

    /**
     * 恢复默认短信应用
     */
    private void RestoreDefaultApp() {
        // 对短信数据库处理结束后，恢复原来的默认SMS APP
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, defaultSmsPkg);
        startActivity(intent);
        Log.i(TAG, "Recover default SMS App");
    }

    /**
     * 更新当前应用为默认短信应用
     */
    private void updateDefaultApp() {
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, mySmsPkg);
        startActivity(intent);

    }

    /**
     * 更新当前显示的收件箱短信值
     */
    private void updateNumMessage() {
        mMessageNumTextView.setText("当前收件箱短信条数：" + getMessageNumInSmsInbox());
    }

    /**
     * 获取收件箱短信条数
     *
     * @return
     */
    private int getMessageNumInSmsInbox() {
        int count = 0;
        Uri uri = Uri.parse(SMS_URI_INBOX);
        Cursor cursor = null;

        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                count = cursor.getCount();
                Log.d(TAG, "count:" + count);
            }

        } catch (SQLiteException ex) {
            Log.d(TAG, ex.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
    }

    /**
     * 异步任务，处理短信填充
     */
    class AddMessageTask extends AsyncTask<Integer, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.setMax(num);
            mProgressDialog.show();
            mIsAdd = true;
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            int num = params[0];

            ContentResolver resolver = getContentResolver();

            String tel = null;
            ArrayList<String> telList = new ArrayList<>();
            boolean isContains = true;
            for (int i = 0; i < num; i++) {
                if (isCancelled()) {
                    break;
                }
                tel = getTel();
                isContains = true;
                while (isContains) {
                    if (!mSmsTelList.contains(tel)) {
                        telList.add(tel);
                        mSmsTelList.add(tel);
                        isContains = false;
                    } else {
                        tel = getTel();
                        isContains = true;
                    }
                }
            }

            Log.d(TAG, telList.toString());
            for (int j = 0; j < telList.size(); j++) {
                if (isCancelled()) {
                    return false;
                }
                ContentValues values = new ContentValues();
                values.put(Telephony.Sms.ADDRESS, telList.get(j));
                values.put(Telephony.Sms.PERSON, "1");
                values.put(Telephony.Sms.DATE, System.currentTimeMillis());
                long dateSent = System.currentTimeMillis() - 5000;
                values.put(Telephony.Sms.DATE_SENT, dateSent);
                values.put(Telephony.Sms.READ, false);
                values.put(Telephony.Sms.SEEN, false);
                values.put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE);
                values.put(Telephony.Sms.BODY, "这是" + telList.get(j) + "的短信！！！");
                values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);

                Uri uri = resolver.insert(Telephony.Sms.CONTENT_URI, values); // 一条一条填充短信
                if (uri == null) {
                    return false;
                }
                publishProgress(j+1);
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (isCancelled()) {
                return;
            }
            mProgressDialog.setIndeterminate(false);//实时更新进度条
            mProgressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            mProgressDialog.dismiss();
            mIsAdd = false;
            if (aBoolean) {
                updateNumMessage();
                Toast.makeText(FillSmsActivity.this, "成功填充短信", Toast.LENGTH_LONG).show();
                mProgressDialog.incrementProgressBy(-mProgressDialog.getProgress());
                RestoreDefaultApp();
            } else {
                updateNumMessage();
                Toast.makeText(FillSmsActivity.this, "填充失败", Toast.LENGTH_LONG).show();
                mProgressDialog.incrementProgressBy(-mProgressDialog.getProgress());
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mProgressDialog.dismiss();
            mIsAdd = false;
            updateNumMessage();
            Toast.makeText(FillSmsActivity.this, "中断了当前填充", Toast.LENGTH_LONG).show();
            mProgressDialog.incrementProgressBy(-mProgressDialog.getProgress());
        }
    }

}
