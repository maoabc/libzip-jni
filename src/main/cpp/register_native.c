/*
    This file was generated automatically by ZipFile.class
*/




#include <jni.h>
#include "mao_archive_libzip_ZipFile.h"


#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

#define CLASS_NAME "mao/archive/libzip/ZipFile"


static JNINativeMethod methods[]={
        {"initIDs","()V",(void *) Java_mao_archive_libzip_ZipFile_initIDs},

        {"open","(Ljava/lang/String;I)J",(void *) Java_mao_archive_libzip_ZipFile_open},

        {"getEntriesCount","(J)J",(void *) Java_mao_archive_libzip_ZipFile_getEntriesCount},

        {"setDefaultPassword0","(JLjava/lang/String;)V",(void *) Java_mao_archive_libzip_ZipFile_setDefaultPassword0},

        {"getDefaultPassword0","(J)Ljava/lang/String;",(void *) Java_mao_archive_libzip_ZipFile_getDefaultPassword0},

        {"removeEntry0","(JJ)Z",(void *) Java_mao_archive_libzip_ZipFile_removeEntry0},

        {"renameEntry","(JJ[B)Z",(void *) Java_mao_archive_libzip_ZipFile_renameEntry},

        {"addFileEntry0","(J[BLjava/lang/String;JJ)J",(void *) Java_mao_archive_libzip_ZipFile_addFileEntry0},

        {"addBufferEntry0","(J[B[B)J",(void *) Java_mao_archive_libzip_ZipFile_addBufferEntry0},

        {"addDirectoryEntry0","(J[B)J",(void *) Java_mao_archive_libzip_ZipFile_addDirectoryEntry0},

        {"nameLocate0","(J[B)J",(void *) Java_mao_archive_libzip_ZipFile_nameLocate0},

        {"openStat0","(JJ)J",(void *) Java_mao_archive_libzip_ZipFile_openStat0},

        {"getName0","(J)[B",(void *) Java_mao_archive_libzip_ZipFile_getName0},

        {"getSize0","(J)J",(void *) Java_mao_archive_libzip_ZipFile_getSize0},

        {"getCSize0","(J)J",(void *) Java_mao_archive_libzip_ZipFile_getCSize0},

        {"getModifyTime0","(J)J",(void *) Java_mao_archive_libzip_ZipFile_getModifyTime0},

        {"getCrc0","(J)J",(void *) Java_mao_archive_libzip_ZipFile_getCrc0},

        {"getCompressionMethod0","(J)I",(void *) Java_mao_archive_libzip_ZipFile_getCompressionMethod0},

        {"getEncryptionMethod0","(J)I",(void *) Java_mao_archive_libzip_ZipFile_getEncryptionMethod0},

        {"freeStat0","(J)V",(void *) Java_mao_archive_libzip_ZipFile_freeStat0},

        {"getComment0","(JJ)[B",(void *) Java_mao_archive_libzip_ZipFile_getComment0},

        {"setModifyTime0","(JJJ)Z",(void *) Java_mao_archive_libzip_ZipFile_setModifyTime0},

        {"setEncryptionMethod0","(JJILjava/lang/String;)Z",(void *) Java_mao_archive_libzip_ZipFile_setEncryptionMethod0},

        {"setCompressionMethod0","(JJII)Z",(void *) Java_mao_archive_libzip_ZipFile_setCompressionMethod0},

        {"setComment0","(JJ[B)Z",(void *) Java_mao_archive_libzip_ZipFile_setComment0},

        {"getZipComment0","(J)[B",(void *) Java_mao_archive_libzip_ZipFile_getZipComment0},

        {"setZipComment0","(J[B)Z",(void *) Java_mao_archive_libzip_ZipFile_setZipComment0},

        {"openEntry","(JJLjava/lang/String;)J",(void *) Java_mao_archive_libzip_ZipFile_openEntry},

        {"readEntryBytes","(J[BII)J",(void *) Java_mao_archive_libzip_ZipFile_readEntryBytes},

        {"closeEntry","(J)V",(void *) Java_mao_archive_libzip_ZipFile_closeEntry},

        {"close","(JLmao/archive/libzip/ProgressListener;)V",(void *) Java_mao_archive_libzip_ZipFile_close},

};


static jboolean registerNativeMethods(JNIEnv* env) {
    jclass clazz = (*env)->FindClass(env, CLASS_NAME);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, methods, NELEM(methods)) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

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