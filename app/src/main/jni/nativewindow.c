#ifndef _jnimediacodec_
#define _jnimediacodec_

#include <jni.h>
#include <log.h>
#include <string.h>
#include <android/native_window_jni.h>
#define LOG_TAG "nativewindow"

#define COLOR_FORMAT_NV21 17
#define COLOR_FORMAT_YV12 0x32315659
JNIEXPORT void JNICALL Java_me_lake_gleslab_MainActivity_ndkdraw
(JNIEnv *env, jobject thiz,jobject javaSurface,jbyteArray pixelsArray,jint w,jint h,jint size)
{
	unsigned char *pixels = (unsigned char*)(*env)->GetByteArrayElements(env,pixelsArray, 0);
	ANativeWindow* window = ANativeWindow_fromSurface(env, javaSurface);
	if(window!=NULL)
	{
	ANativeWindow_setBuffersGeometry(window,w,h,COLOR_FORMAT_YV12);
		ANativeWindow_Buffer buffer;
		if (ANativeWindow_lock(window, &buffer, NULL) == 0) {
					if(buffer.width==buffer.stride){
        				memcpy(buffer.bits, pixels,  size);
        			}else{
        				int height = h*3/2;
        				int width = w;
        				int i=0;
        				for(;i<height;++i)
        					memcpy(buffer.bits +  buffer.stride * i
        						, pixels + width * i
        						, width);
        			}
			ANativeWindow_unlockAndPost(window);
		}
		ANativeWindow_release(window);
	}
	(*env)->ReleaseByteArrayElements(env,pixelsArray,pixels,JNI_ABORT);
	return;
}

#endif
