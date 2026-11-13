cmake_minimum_required(VERSION 3.22.1)
project("liquidisland_gl")

add_library(
    liquidisland_gl
    SHARED
    LiquidGlassGlRenderer.cpp
    liquid_island_jni.cpp
)

find_library(log-lib log)
find_library(gles3-lib GLESv3)
find_library(egl-lib EGL)
find_library(android-lib android)

target_link_libraries(
    liquidisland_gl
    ${log-lib}
    ${gles3-lib}
    ${egl-lib}
    ${android-lib}
)
