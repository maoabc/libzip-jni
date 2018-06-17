INCLUDE(CheckFunctionExists)


set(LIBZIP_SOURCES
  zip_add.c
  zip_add_dir.c
  zip_add_entry.c
  zip_algorithm_deflate.c
  zip_buffer.c
  zip_close.c
  zip_delete.c
  zip_dir_add.c
  zip_dirent.c
  zip_discard.c
  zip_entry.c
  zip_err_str.c
  zip_error.c
  zip_error_clear.c
  zip_error_get.c
  zip_error_get_sys_type.c
  zip_error_strerror.c
  zip_error_to_str.c
  zip_extra_field.c
  zip_extra_field_api.c
  zip_fclose.c
  zip_fdopen.c
  zip_file_add.c
  zip_file_error_clear.c
  zip_file_error_get.c
  zip_file_get_comment.c
  zip_file_get_external_attributes.c
  zip_file_get_offset.c
  zip_file_rename.c
  zip_file_replace.c
  zip_file_set_comment.c
  zip_file_set_encryption.c
  zip_file_set_external_attributes.c
  zip_file_set_mtime.c
  zip_file_strerror.c
  zip_filerange_crc.c
  zip_fopen.c
  zip_fopen_encrypted.c
  zip_fopen_index.c
  zip_fopen_index_encrypted.c
  zip_fread.c
  zip_fseek.c
  zip_ftell.c
  zip_get_archive_comment.c
  zip_get_archive_flag.c
  zip_get_encryption_implementation.c
  zip_get_file_comment.c
  zip_get_name.c
  zip_get_num_entries.c
  zip_get_num_files.c
  zip_hash.c
  zip_io_util.c
  zip_libzip_version.c
  zip_memdup.c
  zip_name_locate.c
  zip_new.c
  zip_open.c
  zip_progress.c
  zip_rename.c
  zip_replace.c
  zip_set_archive_comment.c
  zip_set_archive_flag.c
  zip_set_default_password.c
  zip_set_file_comment.c
  zip_set_file_compression.c
  zip_set_name.c
  zip_source_begin_write.c
  zip_source_begin_write_cloning.c
  zip_source_buffer.c
  zip_source_call.c
  zip_source_close.c
  zip_source_commit_write.c
  zip_source_compress.c
  zip_source_crc.c
  zip_source_error.c
  zip_source_filep.c
  zip_source_free.c
  zip_source_function.c
  zip_source_get_compression_flags.c
  zip_source_is_deleted.c
  zip_source_layered.c
  zip_source_open.c
  zip_source_pkware.c
  zip_source_read.c
  zip_source_remove.c
  zip_source_rollback_write.c
  zip_source_seek.c
  zip_source_seek_write.c
  zip_source_stat.c
  zip_source_supports.c
  zip_source_tell.c
  zip_source_tell_write.c
  zip_source_window.c
  zip_source_write.c
  zip_source_zip.c
  zip_source_zip_new.c
  zip_stat.c
  zip_stat_index.c
  zip_stat_init.c
  zip_strerror.c
  zip_string.c
  zip_unchange.c
  zip_unchange_all.c
  zip_unchange_archive.c
  zip_unchange_data.c
  zip_utf-8.c
)

set(LIBZIP_OPSYS_FILES
    zip_source_file.c
)


include_directories(BEFORE ${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_BINARY_DIR}/..)


add_custom_target(update_zip_err_str
  COMMAND sh ${CMAKE_CURRENT_SOURCE_DIR}/make_zip_err_str.sh ${CMAKE_CURRENT_SOURCE_DIR}/zip.h ${CMAKE_CURRENT_SOURCE_DIR}/zip_err_str.c
  DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/zip.h ${CMAKE_CURRENT_SOURCE_DIR}/make_zip_err_str.sh
  COMMENT "update err str"

)
#add_custom_command(update ddd)
check_function_exists(mkstemp HAVE_MKSTEMP)
if(NOT HAVE_MKSTEMP)
  set(LIBZIP_EXTRA_FILES mkstemp.c)
endif(NOT HAVE_MKSTEMP)
if(HAVE_LIBBZ2)
  set(LIBZIP_OPTIONAL_FILES zip_algorithm_bzip2.c)
endif()

if(HAVE_COMMONCRYPTO)
  set(LIBZIP_OPTIONAL_FILES ${LIBZIP_OPTIONAL_FILES} zip_crypto_commoncrypto.c
)  
elseif(HAVE_GNUTLS)
  set(LIBZIP_OPTIONAL_FILES ${LIBZIP_OPTIONAL_FILES} zip_crypto_gnutls.c
)
elseif(HAVE_OPENSSL)
  set(LIBZIP_OPTIONAL_FILES ${LIBZIP_OPTIONAL_FILES} zip_crypto_openssl.c
)
endif()

if(HAVE_CRYPTO)
  set(LIBZIP_OPTIONAL_FILES ${LIBZIP_OPTIONAL_FILES} zip_winzip_aes.c zip_source_winzip_aes_decode.c zip_source_winzip_aes_encode.c
)
endif()

add_library(zip STATIC ${LIBZIP_SOURCES} ${LIBZIP_EXTRA_FILES} ${LIBZIP_OPTIONAL_FILES} ${LIBZIP_OPSYS_FILES})
set_target_properties(zip PROPERTIES VERSION 5.0 SOVERSION 5)
target_link_libraries(zip ${ZLIB_LIBRARY} ${OPTIONAL_LIBRARY})

