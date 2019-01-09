#
# @author kenping.liu
# @date   2014.08.15
#

LOCAL_PATH := $(call my-dir)


# Prebuilt FFmpeg

include $(CLEAR_VARS)
LOCAL_MODULE:= libavcodec
LOCAL_SRC_FILES:= ./ffmpeg/libavcodec-55.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libavfilter
LOCAL_SRC_FILES:= ./ffmpeg/libavfilter-3.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libavformat
LOCAL_SRC_FILES:= ./ffmpeg/libavformat-55.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libavutil
LOCAL_SRC_FILES:= ./ffmpeg/libavutil-52.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libswresample
LOCAL_SRC_FILES:= ./ffmpeg/libswresample-0.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE:= libswscale
LOCAL_SRC_FILES:= ./ffmpeg/libswscale-2.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
include $(PREBUILT_SHARED_LIBRARY)

# Unisky AV TS Muxer
include $(CLEAR_VARS)
LOCAL_LDLIBS += -llog -landroid -lz
LOCAL_STATIC_LIBRARIES := libavformat libavcodec libswscale libavutil
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_CFLAGS     := -march=armv7-a -mfloat-abi=softfp -mfpu=neon -g -O3
LOCAL_CFLAGS 	 += -D__STDC_CONSTANT_MACROS=1 -D__STDC_LIMIT_MACROS=1
LOCAL_MODULE     := ulavtsmuxer
LOCAL_SRC_FILES  := ulavffmpegts.cpp ulavtsmuxer.cpp

include $(BUILD_SHARED_LIBRARY)
