LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
APP_PLATFORM := android-10
LOCAL_MODULE := rtmp
PATH_RTMP := rtmpdump/librtmp
LOCAL_CFLAGS := -DRTMPDUMP_VERSION=v2.4 -DNO_CRYPTO
LOCAL_LDLIBS := -llog
LOCAL_SRC_FILES := log.c \
$(PATH_RTMP)/rtmp.c \
$(PATH_RTMP)/amf.c \
$(PATH_RTMP)/hashswf.c \
$(PATH_RTMP)/parseurl.c
					
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/rtmpdump
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := rtmpdump
LOCAL_SRC_FILES := rtmpdump-jni.c
LOCAL_SHARED_LIBRARIES := rtmp
include $(BUILD_SHARED_LIBRARY)

SPEEX	:= speex-1.2rc1
include $(CLEAR_VARS)
LOCAL_MODULE    := speex_jni
LOCAL_SRC_FILES := speex_jni.cpp \
		$(SPEEX)/libspeex/speex.c \
		$(SPEEX)/libspeex/speex_callbacks.c \
		$(SPEEX)/libspeex/bits.c \
		$(SPEEX)/libspeex/modes.c \
		$(SPEEX)/libspeex/nb_celp.c \
		$(SPEEX)/libspeex/exc_20_32_table.c \
		$(SPEEX)/libspeex/exc_5_256_table.c \
		$(SPEEX)/libspeex/exc_5_64_table.c \
		$(SPEEX)/libspeex/exc_8_128_table.c \
		$(SPEEX)/libspeex/exc_10_32_table.c \
		$(SPEEX)/libspeex/exc_10_16_table.c \
		$(SPEEX)/libspeex/filters.c \
		$(SPEEX)/libspeex/quant_lsp.c \
		$(SPEEX)/libspeex/ltp.c \
		$(SPEEX)/libspeex/lpc.c \
		$(SPEEX)/libspeex/lsp.c \
		$(SPEEX)/libspeex/vbr.c \
		$(SPEEX)/libspeex/gain_table.c \
		$(SPEEX)/libspeex/gain_table_lbr.c \
		$(SPEEX)/libspeex/lsp_tables_nb.c \
		$(SPEEX)/libspeex/cb_search.c \
		$(SPEEX)/libspeex/vq.c \
		$(SPEEX)/libspeex/window.c \
		$(SPEEX)/libspeex/high_lsp_tables.c

LOCAL_C_INCLUDES += 
LOCAL_CFLAGS = -DFIXED_POINT -DEXPORT="" -UHAVE_CONFIG_H -I$(LOCAL_PATH)/$(SPEEX)/include

include $(BUILD_SHARED_LIBRARY)

