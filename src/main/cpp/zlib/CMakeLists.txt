set(SOURCE_FILES
compress.c
  gzclose.c
  gzread.c
  infback.c
  inflate.c
  trees.c
  adler32.c
  zutil.c
  deflate.c
  gzlib.c
  gzwrite.c
  inffast.c
  inftrees.c
  uncompr.c
  crc32.c
  )

include_directories(${CMAKE_CURRENT_SOURCE_DIR}
  )

add_library (zlib STATIC ${SOURCE_FILES})
set_target_properties(zlib PROPERTIES VERSION 3.0 SOVERSION 3 )
target_link_libraries(zlib )