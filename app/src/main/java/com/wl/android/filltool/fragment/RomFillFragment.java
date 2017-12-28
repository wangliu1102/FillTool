package com.wl.android.filltool.fragment;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
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
import com.wl.android.filltool.util.GetRamRomSdUtil;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by D22397 on 2017/8/28.
 * ROM / SDCard 内存填充
 */

public class RomFillFragment extends Fragment {
    private static final String TAG = "RomFillFragment";

    private File mFile; // ROM路径下文件
    private File mSdFile = null; //外部sd路径下文件

    private EditText mSizeEditText;
    private TextView mRomAllTextView;
    private TextView mRomAvailableTextView;
    private Button mAddRomButton;
    private Button mReleaseRomButton;
    private TextView mSDCardAllTextView;
    private TextView mSDCardAvailableTextView;
    private Button mAddSDCardButton;
    private Button mReleaseSDCardButton;

    private ProgressDialog mProgressDialog;// 填充时的进度框
    private ProgressDialog mProgressDialog2;// 删除时的进度框

    private WriteFileTask writeFileTask = null;
    int mFileSize = 0;
    private boolean mIsAdd = false;
    private boolean mIsDelete = false;
    boolean mIsAddRomOrSd = false; //true是ROM，false是SDCard
    boolean mIsReleaseRomOrSd = false; //true是ROM，false是SDCard
    private List<Integer> mNList = new ArrayList<>(); // 保存SD卡文件标志名
    private int n = 0;

    /**
     * 异步处理释放内存的Handler
     */
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            mProgressDialog2.dismiss();
            mIsDelete = false;
            boolean b = (boolean) msg.obj;
            if (b) {
                if (!mIsReleaseRomOrSd) {
                    mNList.clear();
                }
                Log.d(TAG, "handleMessage: " + mNList.toString());
                updateRom();
                Toast.makeText(getActivity(), "释放成功", Toast.LENGTH_SHORT).show();
            } else {
                updateRom();
                Toast.makeText(getActivity(), "释放失败", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFile = new File("/data/data/" + getActivity().getPackageName() + "/text.txt");
        SharedPreferences preferDataList = getActivity().getSharedPreferences("nList", MODE_PRIVATE);
        int environNums = preferDataList.getInt("nNums", 0);
        for (int i = 0; i < environNums; i++) {
            mNList.add(preferDataList.getInt("item_" + i, 0));
        }
        Log.d(TAG, "onCreate: " + mNList.toString());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_rom_fill, container, false);

        mSizeEditText = (EditText) v.findViewById(R.id.size_edit_text);
        mRomAllTextView = (TextView) v.findViewById(R.id.rom_all_text_view);
        mRomAvailableTextView = (TextView) v.findViewById(R.id.rom_available_text_view);
        mAddRomButton = (Button) v.findViewById(R.id.add_rom_button);
        mReleaseRomButton = (Button) v.findViewById(R.id.release_rom_button);
        mSDCardAllTextView = (TextView) v.findViewById(R.id.sdcard_all_text_view);
        mSDCardAvailableTextView = (TextView) v.findViewById(R.id.sdcard_available_text_view);
        mAddSDCardButton = (Button) v.findViewById(R.id.add_sdcard_button);
        mReleaseSDCardButton = (Button) v.findViewById(R.id.release_sdcard_button);

        if (GetRamRomSdUtil.externalMemoryAvailable(getActivity())) { //是否有外部sd路径
            File f = new File(getActivity().getExternalFilesDir(null).getAbsolutePath());
            if (!f.exists()) {
                f.mkdirs();
            }

            String str = "/Android/data/" + getActivity().getPackageName() + "/files/";
            mSdFile = new File(GetRamRomSdUtil.getStoragePath(getActivity(), true) + str);
            mAddSDCardButton.setEnabled(true);
            mReleaseSDCardButton.setEnabled(true);
        } else {
            mAddSDCardButton.setEnabled(false);
            mReleaseSDCardButton.setEnabled(false);
        }

        updateRom();

        mAddRomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsAddRomOrSd = true;
                addMem();
            }
        });

        mReleaseRomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsReleaseRomOrSd = true;
                releaseMem();
            }
        });

        mAddSDCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsAddRomOrSd = false;
                addMem();
            }
        });

        mReleaseSDCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsReleaseRomOrSd = false;
                releaseMem();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRom();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("nList", MODE_PRIVATE).edit();
        editor.putInt("nNums", mNList.size());
        for (int i = 0; i < mNList.size(); i++) {
            editor.putInt("item_" + i, mNList.get(i));
        }
        editor.apply();
    }

    /**
     * 释放内存
     */
    private void releaseMem() {
        File file = null;
        if (mIsReleaseRomOrSd) { // ROM
            file = mFile;
        } else {
            file = mSdFile;
        }
        if (file.exists() && !mIsDelete) {
            if (mIsReleaseRomOrSd) {
                mProgressDialog2 = ProgressDialog.show(getActivity(), "释放内存",
                        "正在释放ROM内存，请稍后...");
                mIsDelete = true;
                final File finalFile = file;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Boolean b = finalFile.delete();
                        Message msg = new Message();
                        msg.obj = b;
                        mHandler.sendMessageDelayed(msg, 1000);
                    }
                }).start();
            } else {
                File[] files = file.listFiles();
                if (files.length == 0) {
                    Toast.makeText(getActivity(), "正常内存，无需释放", Toast.LENGTH_SHORT).show();
                } else {
                    mProgressDialog2 = ProgressDialog.show(getActivity(), "释放内存",
                            "正在释放SDCard内存，请稍后...");
                    mIsDelete = true;
                    final File finalFile = file;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Boolean b = deleteDir(finalFile);
                            Message msg = new Message();
                            msg.obj = b;
                            mHandler.sendMessageDelayed(msg, 1000);
                        }
                    }).start();
                }
            }

        } else {
            Toast.makeText(getActivity(), "正常内存，无需释放", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 删除目录下的文件
     *
     * @param dir
     */
    public boolean deleteDir(File dir) {
        Boolean b = false;
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            b = files[i].delete();
            if (!b) {
                return b;
            }
        }
        return b;
    }

    /**
     * 填充内存
     */
    private void addMem() {
        String mEdit = mSizeEditText.getText().toString();
        if (!TextUtils.isEmpty(mEdit)) {
            if (mEdit.startsWith("0")) {
                Toast.makeText(getActivity(), "输入不能以 0 开头！", Toast.LENGTH_SHORT).show();
            } else {
                mFileSize = Integer.parseInt(mEdit);
                long availableSize = 0L;
                long size = mFileSize * 1024 * 1024L;
                if (mIsAddRomOrSd) { // 是ROM
                    availableSize = GetRamRomSdUtil.getAvailableInternalStorgeSize();
                } else {
                    availableSize = GetRamRomSdUtil.getAvailableExternalStorgeSize(getActivity());
                }
                Log.d(TAG, "size:" + size);
                if (size < availableSize) {
                    if (!mIsAdd) {
                        writeFileTask = new WriteFileTask();
                        writeFileTask.execute(mFileSize);
                    }

                } else {
                    if (mIsAddRomOrSd) {
                        Toast.makeText(getActivity(), "需填充内存超过可用ROM内存，请重新输入！",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "需填充内存超过可用SDCard内存，请重新输入！",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            Toast.makeText(getActivity(), "请输入填充的大小！！！", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 更新当前显示的ROM值
     */
    private void updateRom() {
        mRomAllTextView.setText("ROM总大小：" +
                GetRamRomSdUtil.formatSize(GetRamRomSdUtil.getTotalInternalStorgeSize()));
        mRomAvailableTextView.setText("ROM可用大小："
                + GetRamRomSdUtil.formatSize(GetRamRomSdUtil.getAvailableInternalStorgeSize()));

        mSDCardAllTextView.setText("SDCard总大小：" +
                GetRamRomSdUtil.formatSize(GetRamRomSdUtil.getTotalExternalStorgeSize(getActivity())));
        mSDCardAvailableTextView.setText("SDCard可用大小：" +
                GetRamRomSdUtil.formatSize(GetRamRomSdUtil.getAvailableExternalStorgeSize(getActivity())));

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
     * 异步任务，处理ROM内存填充
     */
    class WriteFileTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setTitle("填充数据");
            if (mIsAddRomOrSd) {
                mProgressDialog.setMessage("正在填充ROM内存，请稍后...");
            } else {
                mProgressDialog.setMessage("正在填充SDCard内存，请稍后...");
            }
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Stop",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            writeFileTask.cancel(true);
                            writeFileTask = null;
                        }
                    });
            mProgressDialog.setMax(mFileSize);
            mProgressDialog.show();
            mIsAdd = true;
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            int size = params[0];
            try {
                File file = null;
                if (mIsAddRomOrSd) {
                    file = mFile;
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    raf.seek(raf.length());//每次从文件末尾写入
                    for (int i = 0; i < size; i++) {//一共写入size兆,想写多大的文件改变这个值就行
                        if (isCancelled()) {
                            break;
                        }
                        byte[] buffer = new byte[1024 * 1024]; //1次1M，这样内存开的大一些，又不是特别大。
                        raf.write(buffer);
                        publishProgress(i + 1);
                    }
                    raf.close();
                } else {
                    int limitSize = 4 * 1024 - 1; // SDCard保存的文件大小不能超过4G
                    int num = 1;
                    if (size > limitSize) {
                        num = size / limitSize + 1; // 获得要填充的文件数量
                        for (int j = 0; j < num; j++) {
                            while (true) {
                                n = getNum(0, 9999);
                                if (!mNList.contains(n)) {
                                    mNList.add(n);
                                    break;
                                }
                            }
                            file = new File(mSdFile, "abc" + n + ".txt");
                            RandomAccessFile raf = new RandomAccessFile(file, "rw");
                            if (j == (num - 1)) { // 填充最后一个文件
                                for (int i = 0; i < (size % limitSize); i++) {
                                    if (isCancelled()) {
                                        return null;
                                    }
                                    byte[] buffer = new byte[1024 * 1024];
                                    raf.write(buffer);
                                    publishProgress(j * limitSize + (i + 1));
                                }
                                raf.close();
                            } else {
                                for (int i = 0; i < limitSize; i++) {
                                    if (isCancelled()) {
                                        return null;
                                    }
                                    byte[] buffer = new byte[1024 * 1024];
                                    raf.write(buffer);
                                    publishProgress(j * limitSize + (i + 1));
                                }
                                raf.close();
                            }
                        }

                    } else {
                        while (true) {
                            n = getNum(0, 9999);
                            if (!mNList.contains(n)) {
                                mNList.add(n);
                                break;
                            }
                        }
                        file = new File(mSdFile, "abc" + n + ".txt");
                        RandomAccessFile raf = new RandomAccessFile(file, "rw");
                        for (int i = 0; i < size; i++) {//一共写入size兆,想写多大的文件改变这个值就行
                            if (isCancelled()) {
                                break;
                            }
                            byte[] buffer = new byte[1024 * 1024]; //1次1M，这样内存开的大一些，又不是特别大。
                            raf.write(buffer);
                            publishProgress(i + 1);
                        }
                        raf.close();
                    }
                }
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, "doInBackground: " + e.toString());
            }
            return 1;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (isCancelled()) {
                return;
            }
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
                    updateRom();
                    Toast.makeText(getActivity(), "填充成功", Toast.LENGTH_SHORT).show();
                    mProgressDialog.incrementProgressBy(-mProgressDialog.getProgress());
                    break;
                case 1:
                    updateRom();
                    Toast.makeText(getActivity(), "填充失败", Toast.LENGTH_SHORT).show();
                    mProgressDialog.incrementProgressBy(-mProgressDialog.getProgress());
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mProgressDialog.dismiss();
            mIsAdd = false;
            updateRom();
            Toast.makeText(getActivity(), "中断当前填充", Toast.LENGTH_SHORT).show();
            mProgressDialog.incrementProgressBy(-mProgressDialog.getProgress());
        }
    }
}
