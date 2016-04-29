LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


LOCAL_SRC_FILES := nativewindow.c
LOCAL_INCLUDE_FILES := log.h


LOCAL_MODULE := nativewindow


LOCAL_LDLIBS := -llog -ljnigraphics -landroid

include $(BUILD_SHARED_LIBRARY)