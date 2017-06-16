cmake_minimum_required(VERSION 3.5)
project(libzip)

add_subdirectory(lib)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ")

add_library(zip-jni SHARED zip_file.c jni_util.c)

target_link_libraries(zip-jni zipstatic z log)
