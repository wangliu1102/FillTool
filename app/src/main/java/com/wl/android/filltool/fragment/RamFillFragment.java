package com.wl.android.filltool.fragment;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.wl.android.filltool.MemFillTool;
import com.wl.android.filltool.R;
import com.wl.android.filltool.service.RamFillKeepLiveService;
import com.wl.android.filltool.util.GetRamRomSdUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by D22397 on 2017/8/28.
 * RAM内存填充
 */

public class RamFillFragment extends Fragment {

    /**
     * 监听fragment返回键
     */
    protected BackHandlerInterface backHandlerInterface;

    public interface BackHandlerInterface {
        void setSelectedFragment(RamFillFragment backHandledFragment);

        void setIsAddRam(Boolean isAddRam);
    }

    private static final String TAG = "RamFillFragment";


    private EditText mSizeEditText;
    private TextView mRamAllTextView;
    private TextView mRamAvailableTextView;
    private Button mAddRamButton;
    private Button mReleaseRamButton;
    private Button mUpdateRamButton;
    private ProgressDialog mProgressDialog;
    private ProgressDialog mProgressDialog2;

    private MemFillTool mMemFillTool = MemFillTool.getInstance();
    private List<Long> mPList = new ArrayList<>();
    private long mPInt;
    private boolean mHandledPress = false;
    private String mEdit;
    private boolean mIsAdd = false;
    private boolean mBackHandleAddRam = false;
    private boolean mIsDelete = false;
    private boolean mIsClickable = true; // 填充按钮是否可点击
    private boolean mIsStartService = false; // 是否开启服务
    private int size = 0;

    /**
     * 异步处理释放内存的Handler
     */
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            mProgressDialog2.dismiss();
            mIsDelete = false;
            mBackHandleAddRam = false;
            backHandlerInterface.setIsAddRam(mBackHandleAddRam);
            mPList.clear();
            updateRam();
            int b = (int) msg.obj;
            if (b == 0) {
                mAddRamButton.setEnabled(true);
                mIsClickable = true; // 填充按钮可点击
                Toast.makeText(getActivity(), "释放成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "释放失败", Toast.LENGTH_SHORT).show();
            }
        }
    };


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        //回调函数赋值
        if (!(getActivity() instanceof BackHandlerInterface)) {
            throw new ClassCastException("Hosting activity must implement BackHandlerInterface");
        } else {
            backHandlerInterface = (BackHandlerInterface) getActivity();
        }

        // 开启前台服务
        startFroService();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View v = inflater.inflate(R.layout.fragment_ram_fill, container, false);

        mSizeEditText = (EditText) v.findViewById(R.id.ram_size_edit_text);
        mRamAllTextView = (TextView) v.findViewById(R.id.ram_all_text_view);
        mRamAvailableTextView = (TextView) v.findViewById(R.id.ram_available_text_view);
        mAddRamButton = (Button) v.findViewById(R.id.add_ram_button);
        mReleaseRamButton = (Button) v.findViewById(R.id.release_ram_button);
        mUpdateRamButton = (Button) v.findViewById(R.id.update_ram_button);

        updateRam();

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle("填充数据");
        mProgressDialog.setMessage("正在填充RAM内存，请稍后...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);

        mAddRamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 开启前台服务
                startFroService();

                mEdit = mSizeEditText.getText().toString();
                if (!TextUtils.isEmpty(mEdit)) {
                    if (mEdit.startsWith("0")) {
                        Toast.makeText(getActivity(), "输入不能以 0 开头！", Toast.LENGTH_SHORT).show();
                    } else {
                        size = Integer.parseInt(mEdit);
                        if (size * 1024 * 1024L < GetRamRomSdUtil.getAvailMemory(getActivity())) {
                            if (!mIsAdd) {
                                final MemFillTask filltask = new MemFillTask();
                                filltask.execute(size);
                            }
                        } else {
                            updateRam();
                            Toast.makeText(getActivity(), "需填充内存超过可用RAM内存，请重新输入！",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    updateRam();
                    Toast.makeText(getActivity(), "请输入要填充的大小！", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mReleaseRamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Release: " + mPList.toString());
                if (mIsStartService) {
                    // 关闭前台服务
                    Intent intent = new Intent(getActivity(), RamFillKeepLiveService.class);
                    getActivity().stopService(intent);
                    mIsStartService = false;
                    Toast.makeText(getActivity(), "已关闭前台通知服务", Toast.LENGTH_SHORT).show();
                }
                if (mPList.size() != 0 && !mIsDelete) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage("是否释放当前手动已填充的RAM内存！！！")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {
                                    mProgressDialog2 = ProgressDialog.show(getActivity(), "释放内存",
                                            "正在释放RAM内存，请稍后...");
                                    mIsDelete = true;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            int b = 1;
                                            for (int i = 0; i < mPList.size(); i++) {
                                                b = mMemFillTool.freeMem(mPList.get(i));
                                            }
                                            Message msg = new Message();
                                            msg.obj = b;
                                            mHandler.sendMessageDelayed(msg, 1000);
                                        }
                                    }).start();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    updateRam();
                                }
                            })
                            .setCancelable(false)
                            .create().show();

                } else {
                    updateRam();
                    Toast.makeText(getActivity(), "正常内存，无需释放", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mUpdateRamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateRam();
            }
        });


        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
        isBackground(getActivity());
        //将自己的实例传出去
        backHandlerInterface.setSelectedFragment(this);
        backHandlerInterface.setIsAddRam(mBackHandleAddRam);
        updateRam();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        startFroService();

    }

    /**
     * 开启前台服务
     */
    private void startFroService() {
        if (!mIsStartService || !mIsClickable) {
            // 开启前台服务
            Intent intent = new Intent(getContext(), RamFillKeepLiveService.class);
            intent.putExtra("IsClickable", mIsClickable);
            getActivity().startService(intent);
            mIsStartService = true;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
        isBackground(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: ");
        // 销毁该fragment视图时，释放内存
        if (mPList.size() != 0) {
            for (int i = 0; i < mPList.size(); i++) {
                mMemFillTool.freeMem(mPList.get(i));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        // 关闭前台服务
        Intent intent = new Intent(getActivity(), RamFillKeepLiveService.class);
        getActivity().stopService(intent);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach: ");
        mProgressDialog.dismiss();
    }

    /**
     * 自定义监听返回键方法(fragment)
     *
     * @return true:点击 false：未点击
     */
    public boolean onBackPressed() {
        if (!mHandledPress) {
            mHandledPress = true;
            return true;
        }
        return false;
    }

    /**
     * 更新RAM当前显示的值
     */
    private void updateRam() {

        mRamAllTextView.setText("RAM总大小："
                + GetRamRomSdUtil.formatSize(GetRamRomSdUtil.getMemory(getActivity())));
        mRamAvailableTextView.setText("可用RAM大小："
                + GetRamRomSdUtil.formatSize(GetRamRomSdUtil.getAvailMemory(getActivity()))
                + "\n\n" + "最低剩余内存："
                + GetRamRomSdUtil.formatSize(GetRamRomSdUtil.getThresholdMemory(getActivity()))
                + "\n\n" + "是否低内存状态：" + GetRamRomSdUtil.getlowMemory(getActivity()));
    }


    /**
     * 异步任务，进行RAM内存填充
     */
    class MemFillTask extends AsyncTask<Integer, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.setMax(size);
            mProgressDialog.show();
            mIsAdd = true;
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            int size = integers[0];
            try {
                for (int i = 0; i < size; i++) {
                    if (GetRamRomSdUtil.getlowMemory(getActivity())) {
                        return 2;
                    }
                    mPInt = mMemFillTool.fillMem(1);//每次填充1M
                    mPList.add(mPInt);
//                Log.d(TAG, "mPList:" + mPList.toString());
                    publishProgress(i + 1);
                }
                Log.d(TAG, "mPList_length： " + mPList.size());
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
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
        protected void onPostExecute(Integer aInteger) {
            super.onPostExecute(aInteger);
            mProgressDialog.dismiss();
            mIsAdd = false;
            mBackHandleAddRam = true; // 已填充了内存
            backHandlerInterface.setIsAddRam(mBackHandleAddRam);
            switch (aInteger) {
                case 0:
                    updateRam();
                    Toast.makeText(getActivity(), "填充成功", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    updateRam();
                    Toast.makeText(getActivity(), "填充失败", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    mAddRamButton.setEnabled(false);
                    mIsClickable = false; // 填充按钮为不可点击状态
                    updateRam();
                    Toast.makeText(getActivity(), "填充内存达到上限，不能再填充了！",
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    public static boolean isBackground(Context context) {

        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                Log.d(TAG, "isBackground: " + appProcess.importance);
            }
        }
        return false;
    }


}
