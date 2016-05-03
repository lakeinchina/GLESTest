#ifndef _jnimediacodec_
#define _jnimediacodec_

#include <jni.h>
#include <log.h>
#include <string.h>
#include <GLES3/gl3.h>
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

JNIEXPORT void JNICALL Java_me_lake_gleslab_MainActivity_toYV12
(JNIEnv * env, jobject thiz, jbyteArray srcarray, jbyteArray dstarray,jint w,jint h) {
	unsigned char *src = (unsigned char*)(*env)->GetByteArrayElements(env,srcarray, 0);
	unsigned char *dst = (unsigned char*)(*env)->GetByteArrayElements(env,dstarray, 0);
	int ySize=w*h;
	int uvSize=ySize>>1;
	int uSize = uvSize>>1;
	int i=0;
	unsigned char *srcycur = src;
    unsigned char *dstycur = dst;
	while(i<ySize)
	{
		(*dstycur)=(*srcycur);
		++dstycur;
		srcycur+=4;
		++i;
	}
	unsigned char *srcucur = src+1;
    unsigned char *dstvcur = dst+ySize;
    unsigned char *srcvcur = src+2;
    unsigned char *dstucur = dst+ySize+uSize;
    int y=0,x=0;
	for(y=0;y<h;y++)
	{
	    if(y%2==0)//v
	    {
	        srcvcur = src+4*w*y+2;
	        for(x=0;x<w;x+=2)
	        {
	            (*dstvcur)=(*srcvcur);
	            srcvcur+=8;
	            ++dstvcur;
	        }
	    }else//u
	    {
	        srcucur = src+4*w*y+1;
	        for(x=0;x<w;x+=2)
	        {
	            (*dstucur)=(*srcucur);
	            srcucur+=8;
	            ++dstucur;
	        }
	    }
	}
	(*env)->ReleaseByteArrayElements(env,srcarray,src,JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env,dstarray,dst,JNI_ABORT);
	return;
}

JNIEXPORT void JNICALL Java_me_lake_gleslab_MainActivity_readPixel
(JNIEnv *env, jobject thiz,jint f,jint t,jint offset)
{
	glReadPixels(0, 0, 1280, 720, f, t, offset);
}

#endif
