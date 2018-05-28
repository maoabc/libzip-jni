cmake_minimum_required(VERSION 3.5)
project(libzip)

option(ENABLE_GNUTLS "Enable use of GnuTLS" ON)
option(ENABLE_OPENSSL "Enable use of OpenSSL" ON)
option(ENABLE_COMMONCRYPTO "Enable use of CommonCrypto" ON)


include(CheckFunctionExists)
include(CheckIncludeFiles)
include(CheckSymbolExists)
include(CheckTypeSize)
include(CheckCSourceRuns)
include(CheckCSourceCompiles)
include(CheckStructHasMember)
include(TestBigEndian)

if(ENABLE_COMMONCRYPTO)
  check_include_files(CommonCrypto/CommonCrypto.h COMMONCRYPTO_FOUND)
else()
  set(COMMONCRYPTO_FOUND FALSE)
endif()

set(GNUTLS_FOUND FALSE)

if(ENABLE_OPENSSL)
  include(FindOpenSSL)
else()
  set(OPENSSL_FOUND FALSE)
endif()



set(PACKAGE "libzip")
set(PACKAGE_NAME ${PACKAGE})
set(PACKAGE_VERSION_MAJOR "1")
set(PACKAGE_VERSION_MINOR "5")
set(PACKAGE_VERSION_MICRO "0")
#SET(VERSION "${PACKAGE_VERSION_MAJOR}.${PACKAGE_VERSION_MINOR}")
set(VERSION "${PACKAGE_VERSION_MAJOR}.${PACKAGE_VERSION_MINOR}.${PACKAGE_VERSION_MICRO}")
set(PACKAGE_VERSION ${VERSION})
set(LIBZIP_VERSION ${PACKAGE_VERSION})
set(LIBZIP_VERSION_MAJOR ${PACKAGE_VERSION_MAJOR})
set(LIBZIP_VERSION_MINOR ${PACKAGE_VERSION_MINOR})
set(LIBZIP_VERSION_MICRO ${PACKAGE_VERSION_MICRO})
set(PACKAGE_STRING "${PACKAGE_NAME} ${PACKAGE_VERSION}")

set(ARCHIVE_NAME ${PACKAGE_NAME}-${PACKAGE_VERSION})


# Checks

check_function_exists(_chmod HAVE__CHMOD)
check_function_exists(_close HAVE__CLOSE)
check_function_exists(_dup HAVE__DUP)
check_function_exists(_fdopen HAVE__FDOPEN)
check_function_exists(_fileno HAVE__FILENO)
check_function_exists(_open HAVE__OPEN)
check_function_exists(_setmode HAVE__SETMODE)
check_function_exists(_snprintf HAVE__SNPRINTF)
check_function_exists(_strdup HAVE__STRDUP)
check_function_exists(_stricmp HAVE__STRICMP)
check_function_exists(_strtoi64 HAVE__STRTOI64)
check_function_exists(_strtoui64 HAVE__STRTOUI64)
check_function_exists(_umask HAVE__UMASK)
check_function_exists(_unlink HAVE__UNLINK)
check_function_exists(clonefile HAVE_CLONEFILE)
check_function_exists(explicit_bzero HAVE_EXPLICIT_BZERO)
check_function_exists(explicit_memset HAVE_EXPLICIT_MEMSET)
check_function_exists(fileno HAVE_FILENO)
check_function_exists(fseeko HAVE_FSEEKO)
check_function_exists(ftello HAVE_FTELLO)
check_function_exists(getprogname HAVE_GETPROGNAME)
check_function_exists(open HAVE_OPEN)
check_function_exists(mkstemp HAVE_MKSTEMP)
check_function_exists(setmode HAVE_SETMODE)
check_function_exists(snprintf HAVE_SNPRINTF)
check_function_exists(strcasecmp HAVE_STRCASECMP)
check_function_exists(strdup HAVE_STRDUP)
check_function_exists(stricmp HAVE_STRICMP)
check_function_exists(strtoll HAVE_STRTOLL)
check_function_exists(strtoull HAVE_STRTOULL)

check_include_files(fts.h HAVE_FTS_H)
check_include_files(stdbool.h HAVE_STDBOOL_H)
check_include_files(strings.h HAVE_STRINGS_H)
check_include_files(unistd.h HAVE_UNISTD_H)

check_include_files(inttypes.h HAVE_INTTYPES_H_LIBZIP)
check_include_files(stdint.h HAVE_STDINT_H_LIBZIP)
check_include_files(sys/types.h HAVE_SYS_TYPES_H_LIBZIP)


# TODO: fix test
# this test does not find __progname even when it exists
#check_symbol_exists(__progname stdlib.h HAVE___PROGNAME)

check_type_size(__int8 __INT8_LIBZIP)
check_type_size(int8_t INT8_T_LIBZIP)
check_type_size(uint8_t UINT8_T_LIBZIP)
check_type_size(__int16 __INT16_LIBZIP)
check_type_size(int16_t INT16_T_LIBZIP)
check_type_size(uint16_t UINT16_T_LIBZIP)
check_type_size(__int32 __INT32_LIBZIP)
check_type_size(int32_t INT32_T_LIBZIP)
check_type_size(uint32_t UINT32_T_LIBZIP)
check_type_size(__int64 __INT64_LIBZIP)
check_type_size(int64_t INT64_T_LIBZIP)
check_type_size(uint64_t UINT64_T_LIBZIP)
check_type_size("short" SHORT_LIBZIP)
check_type_size("int" INT_LIBZIP)
check_type_size("long" LONG_LIBZIP)
check_type_size("long long" LONG_LONG_LIBZIP)
check_type_size("off_t" SIZEOF_OFF_T)
check_type_size("size_t" SIZE_T_LIBZIP)
check_type_size("ssize_t" SSIZE_T_LIBZIP)

check_c_source_compiles("#include <sys/ioctl.h>
#include <linux/fs.h>
int main(int argc, char *argv[]) { unsigned long x = FICLONERANGE; }" HAVE_FICLONERANGE)

test_big_endian(WORDS_BIGENDIAN)


find_package(ZLIB REQUIRED)
include_directories(${ZLIB_INCLUDE_DIR})
set(CMAKE_REQUIRED_INCLUDES ${ZLIB_INCLUDE_DIR})
if(ZLIB_VERSION_STRING VERSION_LESS "1.1.2")
  message(FATAL_ERROR "-- ZLIB version too old, please install at least v1.1.2")
endif(ZLIB_VERSION_STRING VERSION_LESS "1.1.2")



find_package(BZip2)
if(BZIP2_FOUND)
  set (HAVE_LIBBZ2 1)

  include_directories(${BZIP2_INCLUDE_DIR})
  set (OPTIONAL_LIBRARY ${BZIP2_LIBRARY})
else()
  message(WARNING "-- bzip2 library not found; bzip2 support disabled")
endif(BZIP2_FOUND)

if (COMMONCRYPTO_FOUND)
  set (HAVE_CRYPTO 1)
  set (HAVE_COMMONCRYPTO 1)
elseif (OPENSSL_FOUND)
  set (HAVE_CRYPTO 1)
  set (HAVE_OPENSSL 1)
  include_directories(${OPENSSL_INCLUDE_DIR})
  set (OPTIONAL_LIBRARY ${OPTIONAL_LIBRARY} ${OPENSSL_LIBRARIES})
elseif (GNUTLS_FOUND AND NETTLE_FOUND)
  set (HAVE_CRYPTO 1)
  set (HAVE_GNUTLS 1)
  include_directories(${GNUTLS_INCLUDE_DIR} ${NETTLE_INCLUDE_DIR})
  set (OPTIONAL_LIBRARY ${OPTIONAL_LIBRARY} ${GNUTLS_LIBRARY} ${NETTLE_LIBRARY})
endif()

if (NOT HAVE_CRYPTO)
  message(WARNING "-- neither openssl nor gnutls found; AES support disabled")
endif()

add_definitions("-DHAVE_CONFIG_H")


include_directories(${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_BINARY_DIR})

add_subdirectory(lib)

add_subdirectory(zlib)

if(HAVE_STDINT_H_LIBZIP)
  set(LIBZIP_TYPES_INCLUDE "#include <stdint.h>")
elseif(HAVE_INTTYPES_H_LIBZIP)
  set(LIBZIP_TYPES_INCLUDE "#include <inttypes.h>")
elseif(HAVE_SYS_TYPES_H_LIBZIP)
  set(LIBZIP_TYPES_INCLUDE "#include <sys/types.h>")
endif()

if(HAVE_INT8_T_LIBZIP)
  set(ZIP_INT8_T int8_t)
elseif(HAVE___INT8_LIBZIP)
  set(ZIP_INT8_T __int8)
else()
  set(ZIP_INT8_T "signed char")
endif()

if(HAVE_UINT8_T_LIBZIP)
  set(ZIP_UINT8_T uint8_t)
elseif(HAVE___INT8_LIBZIP)
  set(ZIP_UINT8_T "unsigned __int8")
else()
  set(ZIP_UINT8_T "unsigned char")
endif()

if(HAVE_INT16_T_LIBZIP)
  set(ZIP_INT16_T int16_t)
elseif(HAVE___INT16_LIBZIP)
  set(INT16_T_LIBZIP __int16)
elseif(SHORT_LIBZIP EQUAL 2)
  set(INT16_T_LIBZIP short)
endif()

if(HAVE_UINT16_T_LIBZIP)
  set(ZIP_UINT16_T uint16_t)
elseif(HAVE___INT16_LIBZIP)
  set(UINT16_T_LIBZIP "unsigned __int16")
elseif(SHORT_LIBZIP EQUAL 2)
  set(UINT16_T_LIBZIP "unsigned short")
endif()

if(HAVE_INT32_T_LIBZIP)
  set(ZIP_INT32_T int32_t)
elseif(HAVE___INT32_LIBZIP)
  set(ZIP_INT32_T __int32)
elseif(INT_LIBZIP EQUAL 4)
  set(ZIP_INT32_T int)
elseif(LONG_LIBZIP EQUAL 4)
  set(ZIP_INT32_T long)
endif()

if(HAVE_UINT32_T_LIBZIP)
set(ZIP_UINT32_T uint32_t)
elseif(HAVE___INT32_LIBZIP)
set(ZIP_UINT32_T "unsigned __int32")
elseif(INT_LIBZIP EQUAL 4)
set(ZIP_UINT32_T "unsigned int")
elseif(LONG_LIBZIP EQUAL 4)
set(ZIP_UINT32_T "unsigned long")
endif()

if(HAVE_INT64_T_LIBZIP)
  set(ZIP_INT64_T int64_t)
elseif(HAVE___INT64_LIBZIP)
  set(ZIP_INT64_T __int64)
elseif(LONG_LIBZIP EQUAL 8)
  set(ZIP_INT64_T long)
elseif(LONG_LONG_LIBZIP EQUAL 8)
  set(ZIP_INT64_T "long long")
endif()

if(HAVE_UINT64_T_LIBZIP)
  set(ZIP_UINT64_T uint64_t)
elseif(HAVE___INT64_LIBZIP)
  set(ZIP_UINT64_T "unsigned __int64")
elseif(LONG_LIBZIP EQUAL 8)
  set(ZIP_UINT64_T "unsigned long")
elseif(LONG_LONG_LIBZIP EQUAL 8)
  set(ZIP_UINT64_T "unsigned long long")
endif()


# write out config virtualFile
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/cmake-config.h.in ${CMAKE_CURRENT_BINARY_DIR}/config.h)
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/cmake-zipconf.h.in ${CMAKE_CURRENT_BINARY_DIR}/zipconf.h)





add_library(zip-jni SHARED zip_file.c jni_util.c)

target_link_libraries(zip-jni zip zlib log)
