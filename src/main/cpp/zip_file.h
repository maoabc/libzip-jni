
#ifndef ZIP_FILE_H
#define ZIP_FILE_H

#include <android/log.h>

#define  LOG_TAG    "libzip-jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

void initIDs(JNIEnv *env);

jboolean registerNativeMethods(JNIEnv *env);

#endif
