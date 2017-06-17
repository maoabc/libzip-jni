//
// Created by mao on 16-10-30.
//
#include "lib/zip.h"
#include <string.h>
#include <malloc.h>
#include "mao_archive_libzip_ZipFile.h"
#include "jni_util.h"
#include "jlong.h"
#include "lib/zip.h"
#include <android/log.h>

#define  LOG_TAG    "libzip_jni"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


static jfieldID zip_entry_index_id;
static jfieldID zip_entry_name_bytes_id;
static jfieldID zip_entry_mtime_id;
static jfieldID zip_entry_crc_id;
static jfieldID zip_entry_size_id;
static jfieldID zip_entry_csize_id;
static jfieldID zip_entry_method_id;
static jfieldID zip_entry_emethod_id;
static jfieldID zip_entry_flag_id;

static jmethodID zip_entry_constructor_id;


JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_initIDs
        (JNIEnv *env, jclass cls) {
    jclass zip_entry_cls = (*env)->FindClass(env, "mao/archive/libzip/ZipEntry");
    zip_entry_index_id = (*env)->GetFieldID(env, zip_entry_cls, "index", "J");
    zip_entry_name_bytes_id = (*env)->GetFieldID(env, zip_entry_cls, "rawName", "[B");
    zip_entry_mtime_id = (*env)->GetFieldID(env, zip_entry_cls, "mtime", "J");
    zip_entry_crc_id = (*env)->GetFieldID(env, zip_entry_cls, "crc", "J");
    zip_entry_size_id = (*env)->GetFieldID(env, zip_entry_cls, "size", "J");
    zip_entry_csize_id = (*env)->GetFieldID(env, zip_entry_cls, "csize", "J");
    zip_entry_method_id = (*env)->GetFieldID(env, zip_entry_cls, "method", "I");
    zip_entry_emethod_id = (*env)->GetFieldID(env, zip_entry_cls, "emethod", "I");
    zip_entry_flag_id = (*env)->GetFieldID(env, zip_entry_cls, "flag", "I");
    zip_entry_constructor_id = (*env)->GetMethodID(env, zip_entry_cls, "<init>", "()V");
    (*env)->DeleteLocalRef(env, zip_entry_cls);
}


JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_open(JNIEnv *env, jclass cls, jstring jname,
                                                             jint mode) {
    char errbuf[2048];
    zip_error_t error;
    zip_t *za;
    int err;
    const char *name = (*env)->GetStringUTFChars(env, jname, 0);
    if ((za = zip_open(name, mode, &err)) == NULL) {
        zip_error_init_with_code(&error, err);
        const char *string = zip_error_strerror(&error);
        sprintf(errbuf, "Can't open zip archive '%s': %s\n", name, string);
        zip_error_fini(&error);
        (*env)->ReleaseStringUTFChars(env, jname, name);
        JNU_ThrowIOException(env, errbuf);
        return -1;
    }
    (*env)->ReleaseStringUTFChars(env, jname, name);
    return ptr_to_jlong(za);
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_getEntriesCount
        (JNIEnv *env, jclass cls, jlong jzip) {

    zip_t *za = jlong_to_ptr(jzip);
    zip_int64_t num_entries = zip_get_num_entries(za, ZIP_FL_UNCHANGED);
    if (num_entries < 0) {
        LOGE("get_entries_count error %s", zip_strerror(za));
    }
    return num_entries;
}

JNIEXPORT jobject JNICALL Java_mao_archive_libzip_ZipFile_getEntry
        (JNIEnv *env, jclass cls, jlong jzip, jlong index) {
    zip_stat_t stat;
    jobject entry = NULL;
    jclass entry_cls = NULL;
    zip_t *za = jlong_to_ptr(jzip);

    //ZIP_FL_ENC_RAW 保证不对name进行编码操作
    if (zip_stat_index(za, (zip_uint64_t) index, ZIP_FL_ENC_RAW,
                       &stat) < 0) {
        JNU_ThrowIOException(env, zip_strerror(za));
        return NULL;
    }

    entry_cls = (*env)->FindClass(env, "mao/archive/libzip/ZipEntry");
    if (entry_cls == NULL)
        goto done;

    entry = (*env)->NewObject(env, entry_cls, zip_entry_constructor_id);
    if (entry == NULL)
        goto done;

    //name
    size_t len = strlen(stat.name);
    jbyteArray array = (*env)->NewByteArray(env, (jsize) len);
    (*env)->SetByteArrayRegion(env, array, 0, (jsize) len, (const jbyte *) stat.name);
    (*env)->SetObjectField(env, entry, zip_entry_name_bytes_id, array);

    (*env)->SetLongField(env, entry, zip_entry_index_id, index);

    (*env)->SetLongField(env, entry, zip_entry_mtime_id, stat.mtime);

    (*env)->SetLongField(env, entry, zip_entry_crc_id, stat.crc);

    (*env)->SetLongField(env, entry, zip_entry_size_id, stat.size);

    (*env)->SetLongField(env, entry, zip_entry_csize_id, stat.comp_size);

    (*env)->SetIntField(env, entry, zip_entry_method_id, stat.comp_method);

    (*env)->SetIntField(env, entry, zip_entry_emethod_id, stat.encryption_method);

    (*env)->SetIntField(env, entry, zip_entry_flag_id, stat.flags);


    done:
    (*env)->DeleteLocalRef(env, entry_cls);

    return entry;
}

JNIEXPORT jboolean JNICALL Java_mao_archive_libzip_ZipFile_removeEntry
        (JNIEnv *env, jclass cls, jlong jzip, jlong index) {
    zip_t *za = jlong_to_ptr(jzip);
    if (zip_delete(za, (zip_uint64_t) index) < 0) {
        LOGE("Remove entry error %s", zip_strerror(za));
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_mao_archive_libzip_ZipFile_renameEntry
        (JNIEnv *env, jclass cls, jlong jzip, jlong index, jstring jname) {
    zip_t *za = jlong_to_ptr(jzip);

    const char *name = (*env)->GetStringUTFChars(env, jname, 0);
    if (zip_file_rename(za, (zip_uint64_t) index, name, ZIP_FL_ENC_UTF_8) < 0) {
        LOGE("Rename error %s", zip_strerror(za));
        (*env)->ReleaseStringUTFChars(env, jname, name);
        return JNI_FALSE;
    }
    (*env)->ReleaseStringUTFChars(env, jname, name);
    return JNI_TRUE;

}

JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_addFileEntry
        (JNIEnv *env, jclass cls, jlong jzip, jstring jname, jstring jpath, jint joff, jint jlen) {
    const char *path = NULL;
    const char *name = NULL;
    zip_source_t *source_t = NULL;

    zip_t *za = jlong_to_ptr(jzip);


    path = (*env)->GetStringUTFChars(env, jpath, 0);
    name = (*env)->GetStringUTFChars(env, jname, 0);
    if ((*env)->ExceptionCheck(env))
        goto err;

    source_t = zip_source_file(za, path, (zip_uint64_t) joff, jlen);
    if (source_t == NULL) {
//        LOGE("source error %d", source_t);
        goto err;
    }
//    LOGI("add file %s\n", path);
    if (zip_file_add(za, name, source_t, ZIP_FL_OVERWRITE | ZIP_FL_ENC_UTF_8) < 0) {
//        LOGE("zip error %s", zip_strerror(za));
        goto err;
    }

//    zip_source_free(source_t);

    done:
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    (*env)->ReleaseStringUTFChars(env, jname, name);
    return;

    err:
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    (*env)->ReleaseStringUTFChars(env, jname, name);
    JNU_ThrowIOException(env, zip_strerror(za));
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_addBufferEntry
        (JNIEnv *env, jclass cls, jlong jzip, jstring jname, jbyteArray jbuf) {
    const char *name = NULL;
    char *buf = NULL;
    zip_source_t *zip_source;
    zip_t *za = jlong_to_ptr(jzip);

    jsize length = (*env)->GetArrayLength(env, jbuf);

    buf = malloc((size_t) length);
    if (buf == NULL) {
        JNU_ThrowIOException(env, "Can't malloc memory!");
        return -1;
    }
//    LOGI("malloc %x", buf);
    (*env)->GetByteArrayRegion(env, jbuf, 0, length, (jbyte *) buf);

    name = (*env)->GetStringUTFChars(env, jname, 0);

    //关闭整个zip文件时自动free(buf);
    zip_source = zip_source_buffer(za, buf, (zip_uint64_t) length, 1);
    if (zip_source == NULL) {
        goto err;
    }
//    LOGI("addBuffer %s", name);
    if (zip_file_add(za, name, zip_source, ZIP_FL_OVERWRITE | ZIP_FL_ENC_UTF_8) < 0) {
        goto err;
    }

    (*env)->ReleaseStringUTFChars(env, jname, name);
    return ptr_to_jlong(buf);

    err:
    free(buf);
    (*env)->ReleaseStringUTFChars(env, jname, name);
    JNU_ThrowIOException(env, zip_strerror(za));
    return -1;
}


JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_addDirectoryEntry
        (JNIEnv *env, jclass cls, jlong jzip, jstring jname) {
    zip_t *za = jlong_to_ptr(jzip);

    const char *name = (*env)->GetStringUTFChars(env, jname, 0);

    if (zip_dir_add(za, name, ZIP_FL_ENC_UTF_8) < 0) {
        (*env)->ReleaseStringUTFChars(env, jname, name);
        JNU_ThrowIOException(env, zip_strerror(za));
        return;
    }
    (*env)->ReleaseStringUTFChars(env, jname, name);
}


JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_openEntry
        (JNIEnv *env, jclass cls, jlong jzip, jlong index) {
    zip_t *za = jlong_to_ptr(jzip);


    zip_file_t *zf = zip_fopen_index(za, (zip_uint64_t) index, ZIP_FL_UNCHANGED);
    if (zf == NULL) {
        JNU_ThrowIOException(env, zip_strerror(za));
        return 0;
    }
    return ptr_to_jlong(zf);
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_readEntryBytes
        (JNIEnv *env, jclass cls, jlong jzf, jbyteArray jbuf, jint off, jint jlen) {
    char *buf = NULL;
    zip_file_t *zf = jlong_to_ptr(jzf);

    buf = (*env)->GetPrimitiveArrayCritical(env, jbuf, 0);
    if (buf == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, jbuf, buf, 0);
        JNU_ThrowIOException(env, "Can't get byte array buffer!");
        return -1;
    }

    zip_int64_t len = zip_fread(zf, buf + off, (zip_uint64_t) jlen);
    (*env)->ReleasePrimitiveArrayCritical(env, jbuf, buf, 0);
    return len;

}

JNIEXPORT jint JNICALL Java_mao_archive_libzip_ZipFile_readEntryBuffer
        (JNIEnv *env, jclass cls, jlong jzf, jobject directBuffer, jint jlen) {
    zip_file_t *zf = jlong_to_ptr(jzf);

    char *buffer = (*env)->GetDirectBufferAddress(env, directBuffer);


    if (buffer == NULL) {
        JNU_ThrowIOException(env, "GetDirectBuffer failed");
        return -1;
    }
    zip_int64_t len = zip_fread(zf, buffer, (zip_uint64_t) jlen);
    if (len < 0) {
        return -1;
    }
    return (jint) len;
}


JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_closeEntry
        (JNIEnv *env, jclass cls, jlong jzf) {
    zip_file_t *zf = jlong_to_ptr(jzf);
    if (zip_fclose(zf) != 0) {
        JNU_ThrowIOException(env, "ZipFile close error");
    }
}

static JNIEnv *g_env = NULL;
static jobject g_listener = NULL;
static jmethodID progress_method_id = NULL;

static void progress_callback(double progress) {
//    LOGE("progress %f\n", progress);
    if (g_env != NULL && g_listener != NULL && progress_method_id != NULL) {
        (*g_env)->CallVoidMethod(g_env, g_listener, progress_method_id, (int) (progress * 100));
    }

}

JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_close
        (JNIEnv *env, jclass cls, jlong jzip, jobject listener) {
    zip_t *za = jlong_to_ptr(jzip);
    if (listener != NULL) {
        g_listener = listener;
        if (progress_method_id == NULL) {
            jclass listener_cls = (*env)->GetObjectClass(env, listener);
            progress_method_id = (*env)->GetMethodID(env, listener_cls, "onProgressing", "(I)V");
            (*env)->DeleteLocalRef(env, listener_cls);
            if ((*env)->ExceptionCheck(env)) {
                return;
            }
        }
        if (g_env == NULL) {
            g_env = env;
        }
        zip_register_progress_callback(za, progress_callback);
    }
    if (zip_close(za) < 0) {
        JNU_ThrowIOException(env, zip_strerror(za));
    }
}


