package com.wl.android.filltool;

/**
 * Created by D22397 on 2017/8/22.
 */

public class MemFillTool {

    public static MemFillTool instance;

    static {
        System.loadLibrary("mem_fill_tool");
        instance = null;
    }

    public static MemFillTool getInstance() {
        if (instance == null)
            instance = new MemFillTool();
        return instance;
    }

    public native long fillMem(int paramInt);//RAM内存填充

    public native int freeMem(long mp);//RAM内存释放
}
