package com.wl.android.filltool;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.wl.android.filltool.fragment.FillToolFragment;
import com.wl.android.filltool.fragment.RamFillFragment;

public class FillToolActivity extends FillSingleFragmentActivity
        implements RamFillFragment.BackHandlerInterface {
    private static final String TAG = "FillToolActivity";

    private RamFillFragment selectedFragment;
    private boolean mIsAddRam = false;

    // 定义一个变量，来标识是否退出
    private static boolean isExit = false;
    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }
    };

    @Override
    protected Fragment createFragment() {
        return new FillToolFragment();
    }

    @Override
    public void setSelectedFragment(RamFillFragment backHandledFragment) {
        this.selectedFragment = backHandledFragment;
    }

    @Override
    public void setIsAddRam(Boolean isAddRam) {
        this.mIsAddRam = isAddRam;
    }


    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: " + mIsAddRam);
        if (selectedFragment == null || !selectedFragment.onBackPressed() ||  !mIsAddRam ) { // 监听fragment下的返回键
            if (this.getSupportFragmentManager().getBackStackEntryCount() == 1) {
                exit();
            } else {
                super.onBackPressed();
            }
        } else {
            Log.d(TAG, "onBackPressed: ");
            AlertDialog.Builder builder = new AlertDialog.Builder(FillToolActivity.this);
            builder.setMessage("再点击返回键，内存释放！可点击home键进入后台")
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int which) {

                                }
                            })
//                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(false)
                    .create().show();

        }
    }

    private void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(this, "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            // 利用handler延迟发送更改状态信息
            mHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            finish();
//            System.exit(0);
        }
    }
}
