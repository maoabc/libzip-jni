

#设置openssl静态库目录
set(OPENSSL_DIR ${CMAKE_CURRENT_SOURCE_DIR}/openssl)

if(EXISTS ${OPENSSL_DIR}/${ANDROID_ABI})
  set(OPENSSL_FOUND TRUE)
else()
  set(OPENSSL_FOUND FALSE)
endif()

add_library(lib_crypto STATIC IMPORTED )
set_target_properties(lib_crypto PROPERTIES IMPORTED_LOCATION
    ${OPENSSL_DIR}/${ANDROID_ABI}/libcrypto.a)

add_library(lib_ssl STATIC IMPORTED )
set_target_properties(lib_ssl PROPERTIES IMPORTED_LOCATION
    ${OPENSSL_DIR}/${ANDROID_ABI}/libssl.a)

set(OPENSSL_INCLUDE_DIR ${OPENSSL_DIR}/include)

set(OPENSSL_LIBRARIES lib_crypto lib_ssl)
