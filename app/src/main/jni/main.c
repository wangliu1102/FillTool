#include<com_wl_android_filltool_MemFillTool.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>

#define TAG "MainActivity" // 这个是自定义的LOG的标识
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)  // 定义LOGV类型
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型

static long *p;
JNIEXPORT jlong JNICALL Java_com_wl_android_filltool_MemFillTool_fillMem
        (JNIEnv *env, jobject jobj, jint jparmaInt){
        LOGD("sizeof(long) = %d ", sizeof(long));
        int a;
        if(sizeof(long) == 4 ){
            a = 256;
        } else if(sizeof(long) == 8){
            a = 128;
        }
        LOGD("a = %d ", a);
        p = (long*)malloc(a*1024*jparmaInt*sizeof(long));
        if(p != NULL){
            memset(p,0,a*1024*jparmaInt*sizeof(long));
        }

        return (long)p;
  }

JNIEXPORT jint JNICALL Java_com_wl_android_filltool_MemFillTool_freeMem
        (JNIEnv *env, jobject jobj, jlong jmp){
    p = (long*)jmp;
    free(p);
    p = NULL;
    return 0;
}
