//
// Created by mao on 16-10-30.
//
#include "lib/zip.h"
#include <malloc.h>
#include "mao_archive_libzip_ZipFile.h"
#include "jni_util.h"
#include "jlong.h"
#include "lib/zipint.h"
#include "zip_file.h"

#define MAXNAME 1024

static inline char *getBuffer(JNIEnv *env, jbyteArray jarr, char *stack_buf);

static jmethodID progress_method_id = NULL;

JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_initIDs
        (JNIEnv *env, jclass cls) {
    jclass listener_cls = (*env)->FindClass(env, "mao/archive/libzip/ProgressListener");
    progress_method_id = (*env)->GetMethodID(env, listener_cls, "onProgressing", "(I)V");
    (*env)->DeleteLocalRef(env, listener_cls);
}


JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_open(JNIEnv *env, jclass cls, jstring jname,
                                                             jint mode) {
    zip_error_t error;
    zip_t *za;
    int err;
    const char *name = (*env)->GetStringUTFChars(env, jname, 0);
    if (!name) {
        return 0;
    }
    if ((za = zip_open(name, mode, &err)) == NULL) {
        zip_error_init_with_code(&error, err);
        const char *string = zip_error_strerror(&error);
        zip_error_fini(&error);
        JNU_ThrowIOException(env, string);
    }
    (*env)->ReleaseStringUTFChars(env, jname, name);
    return ptr_to_jlong(za);
}

JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_setDefaultPassword0
        (JNIEnv *env, jclass jcls, jlong jzip, jstring password) {
    zip_t *za = jlong_to_ptr(jzip);
    if (!password) {
        return;
    }
    const char *passwd = (*env)->GetStringUTFChars(env, password, 0);
    if (!passwd) {
        return;
    }
    if (zip_set_default_password(za, passwd) < 0) {
        zip_error_clear(za);
        JNU_ThrowOutOfMemoryError(env, 0);
    }

    (*env)->ReleaseStringUTFChars(env, password, passwd);
}

JNIEXPORT jstring JNICALL Java_mao_archive_libzip_ZipFile_getDefaultPassword0
        (JNIEnv *env, jclass jcls, jlong jzip) {
    zip_t *za = jlong_to_ptr(jzip);

    if (za->default_password) {
        return (*env)->NewStringUTF(env, za->default_password);
    }
    return NULL;
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_getEntriesCount
        (JNIEnv *env, jclass cls, jlong jzip) {

    zip_t *za = jlong_to_ptr(jzip);
    zip_int64_t num_entries = zip_get_num_entries(za, 0);
    if (num_entries < 0) {
        LOGE("get entries count error %s", zip_strerror(za));
        zip_error_clear(za);
    }
    return num_entries;
}

JNIEXPORT jboolean JNICALL Java_mao_archive_libzip_ZipFile_removeEntry0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index) {
    zip_t *za = jlong_to_ptr(jzip);
    if (zip_delete(za, (zip_uint64_t) index) < 0) {
        LOGE("remove entry error %s", zip_strerror(za));
        zip_error_clear(za);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_mao_archive_libzip_ZipFile_renameEntry
        (JNIEnv *env, jclass cls, jlong jzip, jlong index, jbyteArray jrawName) {
    zip_t *za = jlong_to_ptr(jzip);
    char buf[MAXNAME + 2], *newName;

    newName = getBuffer(env, jrawName, buf);
    if (newName == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return JNI_FALSE;
    }

    if (zip_file_rename(za, (zip_uint64_t) index, newName, 0) < 0) {
        LOGE("rename error %s", zip_strerror(za));
        zip_error_clear(za);
        if (newName != buf) {
            free(newName);
        }
        return JNI_FALSE;
    }

    if (newName != buf) {
        free(newName);
    }
    return JNI_TRUE;

}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_addFileEntry0
        (JNIEnv *env, jclass cls, jlong jzip, jbyteArray jrawName, jstring jpath, jlong off,
         jlong len) {
    zip_t *za = jlong_to_ptr(jzip);
    char buf[MAXNAME + 2], *rawName;

    rawName = getBuffer(env, jrawName, buf);
    if (rawName == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return -1;
    }

    const char *path = (*env)->GetStringUTFChars(env, jpath, 0);
    if (path == NULL || (*env)->ExceptionCheck(env))
        goto err;

    zip_source_t *source_t = zip_source_file(za, path, (zip_uint64_t) off, len);
    if (source_t == NULL) {
        goto err;
    }
    zip_int64_t index;
    if ((index = zip_file_add(za, rawName, source_t, ZIP_FL_OVERWRITE)) < 0) {
        zip_source_free(source_t);
        goto err;
    }

    if (rawName != buf) {
        free(rawName);
    }
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    return index;

    err:
    if (rawName != buf) {
        free(rawName);
    }
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    JNU_ThrowIOException(env, zip_strerror(za));
    zip_error_clear(za);
    return -1;
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_addBufferEntry0
        (JNIEnv *env, jclass cls, jlong jzip, jbyteArray jrawName, jbyteArray jdatabuf) {
    zip_t *za = jlong_to_ptr(jzip);
    char buf[MAXNAME + 2], *rawName;


    jsize datalen = (*env)->GetArrayLength(env, jdatabuf);
    //释放zip_source时会自动释放buf
    char *databuf = malloc((size_t) datalen);
    if (databuf == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return -1;
    }
    (*env)->GetByteArrayRegion(env, jdatabuf, 0, datalen, (jbyte *) databuf);

    rawName = getBuffer(env, jrawName, buf);
    if (rawName == NULL) {
        free(databuf);
        JNU_ThrowOutOfMemoryError(env, 0);
        return -1;
    }


    //关闭整个zip文件时自动释放
    zip_source_t *zip_source = zip_source_buffer(za, databuf, (zip_uint64_t) datalen, 1);
    if (zip_source == NULL) {
        goto err;
    }
    zip_int64_t index;
    if ((index = zip_file_add(za, rawName, zip_source, ZIP_FL_OVERWRITE)) < 0) {
        zip_source_free(zip_source);
        goto err;
    }

    if (rawName != buf) {
        free(rawName);
    }
    return index;

    err:
    free(databuf);

    if (rawName != buf) {
        free(rawName);
    }
    JNU_ThrowIOException(env, zip_strerror(za));
    zip_error_clear(za);
    return -1;
}


JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_addDirectoryEntry0
        (JNIEnv *env, jclass cls, jlong jzip, jbyteArray jrawName) {
    zip_t *za = jlong_to_ptr(jzip);
    zip_int64_t index;
    char buf[MAXNAME + 2], *rawName;

    rawName = getBuffer(env, jrawName, buf);
    if (rawName == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return -1;
    }

    if ((index = zip_dir_add(za, rawName, 0)) < 0) {

        JNU_ThrowIOException(env, zip_strerror(za));
        zip_error_clear(za);

    }

    if (rawName != buf) {
        free(rawName);
    }
    return index;
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_nameLocate0
        (JNIEnv *env, jclass cls, jlong jzip, jbyteArray jrawName) {
    zip_t *za = jlong_to_ptr(jzip);
    char buf[MAXNAME + 2], *rawName;

    rawName = getBuffer(env, jrawName, buf);
    if (rawName == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return -1;
    }

    zip_int64_t index;
    if ((index = zip_name_locate(za, rawName, ZIP_FL_ENC_RAW)) < 0) {
        LOGE("name locate %s", zip_strerror(za));
        zip_error_clear(za);
    }

    if (rawName != buf) {
        free(rawName);
    }
    return index;
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_openStat0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index) {
    zip_t *za = jlong_to_ptr(jzip);
    zip_stat_t *stat = malloc(sizeof(zip_stat_t));
    if (!stat) {
        return 0;
    }
    if (zip_stat_index(za, (zip_uint64_t) index, ZIP_FL_ENC_RAW | ZIP_FL_UNCHANGED, stat) < 0) {
        free(stat);
        zip_error_clear(za);
        stat = 0;
    }
    return ptr_to_jlong(stat);
}

JNIEXPORT jbyteArray JNICALL Java_mao_archive_libzip_ZipFile_getName0
        (JNIEnv *env, jclass cls, jlong jstat) {
    zip_stat_t *stat = jlong_to_ptr(jstat);
    size_t len = strlen(stat->name);

    jbyteArray jba = NULL;
    if ((jba = (*env)->NewByteArray(env, len)) == NULL) {
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, jba, 0, len, (jbyte *) stat->name);

    return jba;
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_getSize0
        (JNIEnv *env, jclass cls, jlong jstat) {
    zip_stat_t *stat = jlong_to_ptr(jstat);
    return stat->size;
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_getCSize0
        (JNIEnv *env, jclass cls, jlong jstat) {
    zip_stat_t *stat = jlong_to_ptr(jstat);
    return stat->comp_size != 0 ? stat->comp_size : stat->size;
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_getModifyTime0
        (JNIEnv *env, jclass cls, jlong jstat) {
    zip_stat_t *stat = jlong_to_ptr(jstat);
    return stat->mtime & 0xffffffffUL;
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_getCrc0
        (JNIEnv *env, jclass cls, jlong jstat) {
    zip_stat_t *stat = jlong_to_ptr(jstat);
    return stat->crc & 0xffffffffUL;
}

JNIEXPORT jint JNICALL Java_mao_archive_libzip_ZipFile_getCompressionMethod0
        (JNIEnv *env, jclass cls, jlong jstat) {
    zip_stat_t *stat = jlong_to_ptr(jstat);
    return stat->comp_method;

}

JNIEXPORT jint JNICALL Java_mao_archive_libzip_ZipFile_getEncryptionMethod0
        (JNIEnv *env, jclass cls, jlong jstat) {
    zip_stat_t *stat = jlong_to_ptr(jstat);
    return stat->encryption_method;
}

//JNIEXPORT jint JNICALL Java_mao_archive_libzip_ZipFile_getFlags0
//        (JNIEnv *env, jclass cls, jlong jstat) {
//    zip_stat_t *stat = jlong_to_ptr(jstat);
//    return stat->flags;
//}

JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_freeStat0
        (JNIEnv *env, jclass cls, jlong jstat) {
    zip_stat_t *stat = jlong_to_ptr(jstat);
    free(stat);
}

JNIEXPORT jbyteArray JNICALL Java_mao_archive_libzip_ZipFile_getComment0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index) {
    zip_t *za = jlong_to_ptr(jzip);
    const char *comment = NULL;
    jbyteArray jba = NULL;
    zip_uint32_t len = 0;
    if ((comment = zip_file_get_comment(za, (zip_uint64_t) index, &len, ZIP_FL_ENC_RAW)) == NULL ||
        len == 0) {
        zip_error_clear(za);
        goto done;
    }
    if ((jba = (*env)->NewByteArray(env, len)) == NULL) {
        goto done;
    }
    (*env)->SetByteArrayRegion(env, jba, 0, len, (const jbyte *) comment);
    done:
    return jba;
}

JNIEXPORT jboolean JNICALL Java_mao_archive_libzip_ZipFile_setModifyTime0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index, jlong time) {
    zip_t *za = jlong_to_ptr(jzip);
    if (zip_file_set_mtime(za, (zip_uint64_t) index, (time_t) time, 0) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_mao_archive_libzip_ZipFile_setEncryptionMethod0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index, jint em, jstring password) {
    zip_t *za = jlong_to_ptr(jzip);

    const char *passwd = NULL;
    if (password != NULL) {
        passwd = (*env)->GetStringUTFChars(env, password, 0);
    }
    if (zip_file_set_encryption(za, (zip_uint64_t) index, (zip_uint16_t) em, passwd) < 0) {

        if (passwd) {
            (*env)->ReleaseStringUTFChars(env, password, passwd);
        }
        return JNI_FALSE;
    }

    if (passwd) {
        (*env)->ReleaseStringUTFChars(env, password, passwd);
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_mao_archive_libzip_ZipFile_setCompressionMethod0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index, jint cm, jint level) {
    zip_t *za = jlong_to_ptr(jzip);
    if (zip_set_file_compression(za, (zip_uint64_t) index, cm, (zip_uint32_t) level) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_mao_archive_libzip_ZipFile_setComment0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index, jbyteArray comment) {
    zip_t *za = jlong_to_ptr(jzip);
    char buf[MAXNAME + 2], *comm;

    comm = getBuffer(env, comment, buf);
    if (comm == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return JNI_FALSE;
    }

    if (zip_set_file_comment(za, (zip_uint64_t) index, comm, strlen(comm)) < 0) {
        if (comm != buf) {
            free(comm);
        }
        return JNI_FALSE;
    }

    if (comm != buf) {
        free(comm);
    }
    return JNI_TRUE;
}

JNIEXPORT jbyteArray JNICALL Java_mao_archive_libzip_ZipFile_getZipComment0
        (JNIEnv *env, jclass cls, jlong jzip) {
    zip_t *za = jlong_to_ptr(jzip);
    const char *comment = NULL;
    int len = 0;

    if ((comment = zip_get_archive_comment(za, &len, ZIP_FL_ENC_RAW)) == NULL ||
        len == 0) {
        zip_error_clear(za);
        return NULL;
    }
    jbyteArray jba = NULL;
    if ((jba = (*env)->NewByteArray(env, len)) == NULL) {
        return NULL;
    }

    (*env)->SetByteArrayRegion(env, jba, 0, len, (const jbyte *) comment);

    return jba;
}

JNIEXPORT jboolean JNICALL Java_mao_archive_libzip_ZipFile_setZipComment0
        (JNIEnv *env, jclass cls, jlong jzip, jbyteArray comment) {
    zip_t *za = jlong_to_ptr(jzip);
    char buf[MAXNAME + 2], *comm;

    comm = getBuffer(env, comment, buf);
    if (comm == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return JNI_FALSE;
    }


    if (zip_set_archive_comment(za, comm, (zip_uint16_t) strlen(comm)) < 0) {

        if (comm != buf) {
            free(comm);
        }
        zip_error_clear(za);
        return JNI_FALSE;
    }

    if (comm != buf) {
        free(comm);
    }
    return JNI_TRUE;
}

static inline char *getBuffer(JNIEnv *env, jbyteArray jarr, char *stack_buf) {
    char *buf;
    jsize ulen = (*env)->GetArrayLength(env, jarr);
    if (ulen > MAXNAME) {
        buf = malloc((size_t) (ulen + 2));
        if (buf == NULL) {
            return NULL;
        }
    } else {
        buf = stack_buf;
    }
    (*env)->GetByteArrayRegion(env, jarr, 0, ulen, (jbyte *) buf);
    buf[ulen] = 0;
    return buf;

}


JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_openEntry
        (JNIEnv *env, jclass cls, jlong jzip, jlong index, jstring password) {
    zip_t *za = jlong_to_ptr(jzip);

    char *passwd = NULL;

    if (password != NULL) {
        passwd = (char *) (*env)->GetStringUTFChars(env, password, 0);
    }


    zip_file_t *zf = zip_fopen_index_encrypted(za, (zip_uint64_t) index, ZIP_FL_UNCHANGED, passwd);
    if (zf == NULL) {
        switch (za->error.zip_err) {
            case ZIP_ER_WRONGPASSWD:
            case ZIP_ER_NOPASSWD: {
                JNU_ThrowByNameWithLastError(env, "mao/archive/libzip/WrongPasswordException",
                                             zip_strerror(za));
                break;
            }
            case ZIP_ER_COMPNOTSUPP:
            case ZIP_ER_ENCRNOTSUPP: {
                JNU_ThrowByNameWithLastError(env, "java/util/zip/ZipException", zip_strerror(za));
                break;
            }
            default:
                JNU_ThrowIOExceptionWithLastError(env, zip_strerror(za));
        }
        zip_error_clear(za);
    }
    if (passwd != NULL) {
        (*env)->ReleaseStringUTFChars(env, password, passwd);
    }
    return ptr_to_jlong(zf);
}

JNIEXPORT jlong JNICALL Java_mao_archive_libzip_ZipFile_readEntryBytes
        (JNIEnv *env, jclass cls, jlong jzf, jbyteArray bytes, jint off, jint len) {
    zip_file_t *zf = jlong_to_ptr(jzf);

#define BUFSIZE 8192

    jbyte buf[BUFSIZE];

    if (len > BUFSIZE) {
        len = BUFSIZE;
    }


    zip_int64_t l = zip_fread(zf, buf, (zip_uint64_t) len);

    if (l != -1) {
        (*env)->SetByteArrayRegion(env, bytes, off, (jsize) l, buf);
    }

    return l;

}


JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_closeEntry
        (JNIEnv *env, jclass cls, jlong jzf) {
    zip_file_t *zf = jlong_to_ptr(jzf);
    if (zip_fclose(zf)) {
        JNU_ThrowIOException(env, "close error");
    }
}
JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_discard0
        (JNIEnv *env, jclass cls, jlong jzip){
    LOGI("discard");
    zip_t *za = jlong_to_ptr(jzip);
    zip_discard(za);
}


struct user_data {
    JNIEnv *env;
    jobject listener;
};


static void progress_callback(zip_t *za, double percent, void *vud) {
    struct user_data *data = vud;
//    LOGI("percent %f ", percent);
    if (data->env && data->listener && progress_method_id) {
        (*data->env)->CallVoidMethod(data->env, data->listener, progress_method_id,
                                     (jint) (percent * 100));
    }

}

static void ud_free(void *vud) {
    struct user_data *data = vud;
    if (data->env && data->listener) {
        (*data->env)->DeleteLocalRef(data->env, data->listener);
    }
}


JNIEXPORT void JNICALL Java_mao_archive_libzip_ZipFile_close0
        (JNIEnv *env, jclass cls, jlong jzip, jobject listener) {
    struct user_data data;
    zip_t *za = jlong_to_ptr(jzip);
    if (za == NULL) {
        return;
    }
    if (listener) {
        data.env = env;
        data.listener = listener;
        zip_register_progress_callback_with_state(za, 0.02, progress_callback, ud_free, &data);
    }

    if (zip_close(za) < 0) {
        JNU_ThrowIOException(env, zip_strerror(za));
    }
}


