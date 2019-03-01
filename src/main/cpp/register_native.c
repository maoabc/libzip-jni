/*
    This file was generated automatically by ZipFile.class
*/




#include <jni.h>
#include "zip_file.h"

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = NULL;

    if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }
    if (!registerNativeMethods(env)) {
        return -1;
    }

    return JNI_VERSION_1_4;
}