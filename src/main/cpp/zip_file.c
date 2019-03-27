//
// Created by mao on 16-10-30.
//
#include "lib/zip.h"
#include <malloc.h>
#include "jni_util.h"
#include "jlong.h"
#include "lib/zipint.h"
#include "zip_file.h"

#define MAXNAME 1024

static inline char *getBuffer(JNIEnv *env, jbyteArray jarr, char *stack_buf);

static void initConstant(JNIEnv *env, jclass c, const char *fieldName, int value) {
    jfieldID field = (*env)->GetStaticFieldID(env, c, fieldName, "I");
    (*env)->SetStaticIntField(env, c, field, value);
}

static jmethodID progress_method;

static jclass zipEntryClass;
static jmethodID zipEntry_ctor;

void initIDs(JNIEnv *env) {
    jclass listener_cls = (*env)->FindClass(env, "mao/archive/libzip/ProgressListener");
    if (listener_cls == NULL) {
        return;
    }
    progress_method = (*env)->GetMethodID(env, listener_cls, "onProgressing", "(I)V");

    zipEntryClass = (*env)->NewGlobalRef(env,
                                         (*env)->FindClass(env, "mao/archive/libzip/ZipEntry"));

    zipEntry_ctor = (*env)->GetMethodID(env, zipEntryClass, "<init>",
                                        "(JLmao/archive/libzip/ZipCoder;[B" "JJJJII[B[B)V");

    jclass zipFileClass = (*env)->FindClass(env, "mao/archive/libzip/ZipFile");
    initConstant(env, zipFileClass, "ZIP_CREATE", ZIP_CREATE);
    initConstant(env, zipFileClass, "ZIP_EXCL", ZIP_EXCL);
    initConstant(env, zipFileClass, "ZIP_CHECKCONS", ZIP_CHECKCONS);
    initConstant(env, zipFileClass, "ZIP_TRUNCATE", ZIP_TRUNCATE);
    initConstant(env, zipFileClass, "ZIP_RDONLY", ZIP_RDONLY);

    initConstant(env, zipFileClass, "ZIP_EM_NONE", ZIP_EM_NONE);
    initConstant(env, zipFileClass, "ZIP_EM_TRAD_PKWARE", ZIP_EM_TRAD_PKWARE);
    initConstant(env, zipFileClass, "ZIP_EM_AES_128", ZIP_EM_AES_128);
    initConstant(env, zipFileClass, "ZIP_EM_AES_192", ZIP_EM_AES_192);
    initConstant(env, zipFileClass, "ZIP_EM_AES_256", ZIP_EM_AES_256);

    initConstant(env, zipFileClass, "ZIP_CM_DEFAULT", ZIP_CM_DEFAULT);
    initConstant(env, zipFileClass, "ZIP_CM_STORE", ZIP_CM_STORE);
    initConstant(env, zipFileClass, "ZIP_CM_DEFLATE", ZIP_CM_DEFLATE);
    initConstant(env, zipFileClass, "ZIP_CM_BZIP2", ZIP_CM_BZIP2);


    (*env)->DeleteLocalRef(env, zipFileClass);

    (*env)->DeleteLocalRef(env, listener_cls);
}


static jlong Java_mao_archive_libzip_ZipFile_open(JNIEnv *env, jclass cls, jstring jname,
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

static void Java_mao_archive_libzip_ZipFile_setDefaultPassword0
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

static jstring Java_mao_archive_libzip_ZipFile_getDefaultPassword0
        (JNIEnv *env, jclass jcls, jlong jzip) {
    zip_t *za = jlong_to_ptr(jzip);

    if (za->default_password) {
        return (*env)->NewStringUTF(env, za->default_password);
    }
    return NULL;
}

static jlong Java_mao_archive_libzip_ZipFile_getEntriesCount
        (JNIEnv *env, jclass cls, jlong jzip) {

    zip_t *za = jlong_to_ptr(jzip);
    zip_int64_t num_entries = zip_get_num_entries(za, 0);
    if (num_entries < 0) {
        LOGE("get entries count error %s", zip_strerror(za));
        zip_error_clear(za);
    }
    return num_entries;
}

static jboolean Java_mao_archive_libzip_ZipFile_removeEntry0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index) {
    zip_t *za = jlong_to_ptr(jzip);
    if (zip_delete(za, (zip_uint64_t) index) < 0) {
        LOGE("remove entry error %s", zip_strerror(za));
        zip_error_clear(za);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static jboolean Java_mao_archive_libzip_ZipFile_renameEntry
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

static jlong Java_mao_archive_libzip_ZipFile_addFileEntry0
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

static jlong Java_mao_archive_libzip_ZipFile_addBufferEntry0
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


static jlong Java_mao_archive_libzip_ZipFile_addDirectoryEntry0
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

static jlong Java_mao_archive_libzip_ZipFile_nameLocate0
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

static jobject Java_mao_archive_libzip_ZipFile_getEntry0
        (JNIEnv *env, jclass cls, jlong jzip, jobject zc, jlong index) {
    zip_t *za = jlong_to_ptr(jzip);
    const char *comment;

    zip_stat_t stat;

    if (zip_stat_index(za, (zip_uint64_t) index, ZIP_FL_ENC_RAW | ZIP_FL_UNCHANGED, &stat) < 0) {
        zip_error_clear(za);
        return NULL;
    }
    //entry name
    size_t nameLen = strlen(stat.name);
    jbyteArray rawName = (*env)->NewByteArray(env, (jsize) nameLen);
    if (rawName == NULL) {
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, rawName, 0, (jsize) nameLen, (const jbyte *) stat.name);


    //comment
    zip_uint32_t commentLen = 0;
    if ((comment = zip_file_get_comment(za, (zip_uint64_t) index, &commentLen, ZIP_FL_ENC_RAW)) ==
        NULL) {
        zip_error_clear(za);
        return NULL;
    }

    jbyteArray rawComment = (*env)->NewByteArray(env, commentLen);
    if (rawComment == NULL) {
        return NULL;
    }
    (*env)->SetByteArrayRegion(env, rawComment, 0, (jsize) commentLen, (const jbyte *) comment);


    return (*env)->NewObject(env,
                             zipEntryClass, zipEntry_ctor, index, zc, rawName,
                             stat.mtime, stat.crc, stat.size, stat.comp_size,
                             stat.comp_method, stat.encryption_method,
                             NULL, rawComment);

}


static jboolean Java_mao_archive_libzip_ZipFile_setModifyTime0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index, jlong time) {
    zip_t *za = jlong_to_ptr(jzip);
    if (zip_file_set_mtime(za, (zip_uint64_t) index, (time_t) time, 0) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static jboolean Java_mao_archive_libzip_ZipFile_setEncryptionMethod0
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

static jboolean Java_mao_archive_libzip_ZipFile_setCompressionMethod0
        (JNIEnv *env, jclass cls, jlong jzip, jlong index, jint cm, jint level) {
    zip_t *za = jlong_to_ptr(jzip);
    if (zip_set_file_compression(za, (zip_uint64_t) index, cm, (zip_uint32_t) level) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static jboolean Java_mao_archive_libzip_ZipFile_setComment0
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

static jbyteArray Java_mao_archive_libzip_ZipFile_getZipComment0
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

static jboolean Java_mao_archive_libzip_ZipFile_setZipComment0
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


static jlong Java_mao_archive_libzip_ZipFile_openEntry
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
                JNU_ThrowByNameWithLastError(env, "mao/archive/libzip/PasswordException",
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

static jlong Java_mao_archive_libzip_ZipFile_readEntryBytes
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


static void Java_mao_archive_libzip_ZipFile_closeEntry
        (JNIEnv *env, jclass cls, jlong jzf) {
    zip_file_t *zf = jlong_to_ptr(jzf);
    if (zip_fclose(zf)) {
        JNU_ThrowIOException(env, "close error");
    }
}

static void Java_mao_archive_libzip_ZipFile_discard0
        (JNIEnv *env, jclass cls, jlong jzip) {
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
    if (data->env && data->listener && progress_method) {
        (*data->env)->CallVoidMethod(data->env, data->listener, progress_method,
                                     (jint) (percent * 100));
    }

}

static void ud_free(void *vud) {
    struct user_data *data = vud;
    if (data->env && data->listener) {
        (*data->env)->DeleteLocalRef(data->env, data->listener);
    }
}


static void Java_mao_archive_libzip_ZipFile_close0
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

#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

static JNINativeMethod methods[] = {
        {"open",                  "(Ljava/lang/String;I)J",                                         (void *) Java_mao_archive_libzip_ZipFile_open},

        {"getEntriesCount",       "(J)J",                                                           (void *) Java_mao_archive_libzip_ZipFile_getEntriesCount},

        {"setDefaultPassword0",   "(JLjava/lang/String;)V",                                         (void *) Java_mao_archive_libzip_ZipFile_setDefaultPassword0},

        {"getDefaultPassword0",   "(J)Ljava/lang/String;",                                          (void *) Java_mao_archive_libzip_ZipFile_getDefaultPassword0},

        {"removeEntry0",          "(JJ)Z",                                                          (void *) Java_mao_archive_libzip_ZipFile_removeEntry0},

        {"renameEntry",           "(JJ[B)Z",                                                        (void *) Java_mao_archive_libzip_ZipFile_renameEntry},

        {"addFileEntry0",         "(J[BLjava/lang/String;JJ)J",                                     (void *) Java_mao_archive_libzip_ZipFile_addFileEntry0},

        {"addBufferEntry0",       "(J[B[B)J",                                                       (void *) Java_mao_archive_libzip_ZipFile_addBufferEntry0},

        {"addDirectoryEntry0",    "(J[B)J",                                                         (void *) Java_mao_archive_libzip_ZipFile_addDirectoryEntry0},

        {"nameLocate0",           "(J[B)J",                                                         (void *) Java_mao_archive_libzip_ZipFile_nameLocate0},

        {"getEntry0",             "(JLmao/archive/libzip/ZipCoder;J)Lmao/archive/libzip/ZipEntry;", (void *) Java_mao_archive_libzip_ZipFile_getEntry0},

        {"setModifyTime0",        "(JJJ)Z",                                                         (void *) Java_mao_archive_libzip_ZipFile_setModifyTime0},

        {"setEncryptionMethod0",  "(JJILjava/lang/String;)Z",                                       (void *) Java_mao_archive_libzip_ZipFile_setEncryptionMethod0},

        {"setCompressionMethod0", "(JJII)Z",                                                        (void *) Java_mao_archive_libzip_ZipFile_setCompressionMethod0},

        {"setComment0",           "(JJ[B)Z",                                                        (void *) Java_mao_archive_libzip_ZipFile_setComment0},

        {"getZipComment0",        "(J)[B",                                                          (void *) Java_mao_archive_libzip_ZipFile_getZipComment0},

        {"setZipComment0",        "(J[B)Z",                                                         (void *) Java_mao_archive_libzip_ZipFile_setZipComment0},

        {"openEntry",             "(JJLjava/lang/String;)J",                                        (void *) Java_mao_archive_libzip_ZipFile_openEntry},

        {"readEntryBytes",        "(J[BII)J",                                                       (void *) Java_mao_archive_libzip_ZipFile_readEntryBytes},

        {"closeEntry",            "(J)V",                                                           (void *) Java_mao_archive_libzip_ZipFile_closeEntry},

        {"discard0",              "(J)V",                                                           (void *) Java_mao_archive_libzip_ZipFile_discard0},

        {"close0",                "(JLmao/archive/libzip/ProgressListener;)V",                      (void *) Java_mao_archive_libzip_ZipFile_close0},

};


jboolean registerNativeMethods(JNIEnv *env) {
    jclass clazz = (*env)->FindClass(env, "mao/archive/libzip/ZipFile");
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, methods, NELEM(methods)) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
