package com.wl.android.filltool.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by D22397 on 2017/8/16.
 * 获取手机内存信息RAM、ROM、SD
 */

public class GetRamRomSdUtil {

    /**
     * 获取手机内存(RAM)
     *
     * @return
     */
    public static long getMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.totalMem;
    }

    /**
     * 获取可用手机内存(RAM)
     *
     * @return
     */
    public static long getAvailMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }

    /**
     * 获取设置-》正在运行服务-》正在缓存的进程-》free内存
     *
     * @return
     */
    public static long getSystemCachedProcessesAvailMemory() {
        return getMemFree() + getMemCached() + getBuffers() - getMapped();
    }

    /**
     * 获得/proc/meminfo下的内存信息（可用RAM：MemAvailable = MemFree + Cached）
     *
     * @return
     */
    public static String getMemInfo() {
        BufferedReader bufferedReader = null;
        long memFree = 0L;
        long memAvailable = 0L;
        long cached = 0L;
        String str;
        try {
            final String mem_path = "/proc/meminfo";
            int i = 1;
            bufferedReader = new BufferedReader(new FileReader(mem_path));
            while ((str = bufferedReader.readLine()) != null) {
                if (i == 2) {
                    Log.d("MemInfo", "MemFree: " + str);
                    str = str.split("\\s+")[1];
                    memFree = Long.parseLong(str) * 1024L;
                }
                if (i == 3) {
                    Log.d("MemInfo", "MemAvailable: " + str);
                    str = str.split("\\s+")[1];
                    memAvailable = Long.parseLong(str) * 1024L;
                }
                if (i == 5) {
                    Log.d("MemInfo", "Cached: " + str);
                    str = str.split("\\s+")[1];
                    cached = Long.parseLong(str) * 1024L;
                }
                i++;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "\n" + "MemFree: " + formatSize(memFree) + "\n\n" +
                "Cached: " + formatSize(cached);
    }

    /**
     * 获得MemFree内存大小（字节）
     *
     * @return
     */
    public static long getMemFree() {
        BufferedReader bufferedReader = null;
        long memFree = 0L;
        String str;
        try {
            final String mem_path = "/proc/meminfo";
            int i = 1;
            bufferedReader = new BufferedReader(new FileReader(mem_path));
            while ((str = bufferedReader.readLine()) != null) {
                if (i == 2) {
                    Log.d("MemInfo", "MemFree: " + str);
                    str = str.split("\\s+")[1];
                    memFree = Long.parseLong(str) * 1024L;
//                    Log.d("MemInfo", "float: " + formatSize(memFree).substring(0,formatSize(memFree).indexOf("B")-1));
                    break;
                }
                i++;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        float free = Float.parseFloat(formatSize(memFree).substring(0,formatSize(memFree).indexOf("B")-1));
        return memFree;
    }

    /**
     * 获得MemAvailable内存大小（字节）
     *
     * @return
     */
    public static long getMemAvailable() {
        BufferedReader bufferedReader = null;
        long memAvailable = 0L;
        String str;
        try {
            final String mem_path = "/proc/meminfo";
            int i = 1;
            bufferedReader = new BufferedReader(new FileReader(mem_path));
            while ((str = bufferedReader.readLine()) != null) {
                if (i == 3) {
                    Log.d("MemInfo", "MemAvailable: " + str);
                    str = str.split("\\s+")[1];
                    memAvailable = Long.parseLong(str) * 1024L;
                    break;
                }
                i++;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return memAvailable;
    }

    /**
     * 获得Buffers内存大小（字节）
     *
     * @return
     */
    public static long getBuffers() {
        BufferedReader bufferedReader = null;
        long buffers = 0L;
        String str;
        try {
            final String mem_path = "/proc/meminfo";
            int i = 1;
            bufferedReader = new BufferedReader(new FileReader(mem_path));
            while ((str = bufferedReader.readLine()) != null) {
                if (i == 4) {
                    Log.d("MemInfo", "Buffers: " + str);
                    str = str.split("\\s+")[1];
                    buffers = Long.parseLong(str) * 1024L;
                    break;
                }
                i++;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffers;
    }

    /**
     * 获取Cached内存大小（字节）
     *
     * @return
     */
    public static long getMemCached() {
        BufferedReader bufferedReader = null;
        long cached = 0L;
        String str;
        try {
            final String mem_path = "/proc/meminfo";
            int i = 1;
            bufferedReader = new BufferedReader(new FileReader(mem_path));
            while ((str = bufferedReader.readLine()) != null) {
                if (i == 5) {
                    Log.d("MemInfo", "Cached: " + str);
                    str = str.split("\\s+")[1];
                    cached = Long.parseLong(str) * 1024L;
//                    Log.d("MemInfo", "float: " + formatSize(cached).substring(0,formatSize(cached).indexOf("B")-1));
                    break;
                }
                i++;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        float c = Float.parseFloat(formatSize(cached).substring(0,formatSize(cached).indexOf("B")-1));
        return cached;
    }

    /**
     * 获得Mapped内存大小（字节）
     *
     * @return
     */
    public static long getMapped() {
        BufferedReader bufferedReader = null;
        long mapped = 0L;
        String str;
        try {
            final String mem_path = "/proc/meminfo";
            int i = 1;
            bufferedReader = new BufferedReader(new FileReader(mem_path));
            while ((str = bufferedReader.readLine()) != null) {
                if (i == 20) {
                    Log.d("MemInfo", "Mapped: " + str);
                    str = str.split("\\s+")[1];
                    mapped = Long.parseLong(str) * 1024L;
                    break;
                }
                i++;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mapped;
    }

    /**
     * 获取手机最低剩余内存，当系统剩余内存低于 “threshold” 时就看成低内存运行，就会有可能触发LMK，系统开始杀进程了
     *
     * @return
     */
    public static long getThresholdMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.threshold;
    }

    /**
     * 判断是否处于低内存状态
     *
     * @return
     */
    public static boolean getlowMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.lowMemory;
    }

    /**
     * 获取手机内部空间大小(ROM)
     *
     * @return
     */
    public static long getTotalInternalStorgeSize() {
        File path = Environment.getDataDirectory();
        StatFs mStatFs = new StatFs(path.getPath());
        long blockSize = mStatFs.getBlockSizeLong();
        long totalBlocks = mStatFs.getBlockCountLong();
        return totalBlocks * blockSize;
    }

    /**
     * 获取手机内部可用空间大小(ROM)
     *
     * @return
     */
    public static long getAvailableInternalStorgeSize() {
        File path = Environment.getDataDirectory();
        Log.d("GetRamRomSdUtil", "getAvailableInternalStorgeSize: " + path.getPath());
        StatFs mStatFs = new StatFs(path.getPath());
        long blockSize = mStatFs.getBlockSizeLong();
        long availableBlocks = mStatFs.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    /**
     * 获取内外SD卡路径
     *
     * @param mContext
     * @param is_removale true时是外部存储，false是内部存储
     * @return
     */
    public static String getStoragePath(Context mContext, boolean is_removale) {

        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断是否有外部存储路径，SDCard1，(默认存储为内部存储路径SDCard0)
     *
     * @param mContext
     * @return
     */
    public static boolean externalMemoryAvailable(Context mContext) {
        if (getStoragePath(mContext, true) != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取手机外部空间大小(SDCard)
     *
     * @return
     */
    public static long getTotalExternalStorgeSize(Context mContext) {
        if (externalMemoryAvailable(mContext)) {
            String path = getStoragePath(mContext, true);
            StatFs mStatFs = new StatFs(path);
            long blockSize = mStatFs.getBlockSizeLong();
            long totalBlocks = mStatFs.getBlockCountLong();
            return totalBlocks * blockSize;
        } else {
            return 0;
        }
    }

    /**
     * 获取手机外部可用空间大小(SDCard)
     *
     * @return
     */
    public static long getAvailableExternalStorgeSize(Context mContext) {
        if (externalMemoryAvailable(mContext)) {
            String path = getStoragePath(mContext, true);
            StatFs mStatFs = new StatFs(path);
            long blockSize = mStatFs.getBlockSizeLong();
            long availableBlocks = mStatFs.getAvailableBlocksLong();
            return availableBlocks * blockSize;
        } else {
            return 0;
        }
    }

    /**
     * 外部存储(SDCard)是否可用
     *
     * @return
     */
    public static boolean externalMemoryAvailable() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /**
     * 数据进行格式化
     *
     * @param size
     * @return
     */
    public static String formatSize(long size) {
        String suffix = "";
        float fSzie = 0;
        if (size >= 1024) {
            suffix = "KB";
            fSzie = size / 1024;
            if (fSzie >= 1024) {
                suffix = "MB";
                fSzie /= 1024;
                if (fSzie >= 1024) {
                    suffix = "GB";
                    fSzie /= 1024;
                }
            }
        }

        DecimalFormat formatter = new DecimalFormat("#0.00");// 字符显示格式
        /* 每3个数字用,分隔，如1,000 */
        formatter.setGroupingSize(3);
        StringBuilder resultBuffer = new StringBuilder(formatter.format(fSzie));
        if (suffix != null) {
            resultBuffer.append(suffix);
        }
        return resultBuffer.toString();
    }
}
