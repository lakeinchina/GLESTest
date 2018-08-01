#ifndef _jnimediacodec_
#define _jnimediacodec_

#include <jni.h>
#include <log.h>
#include <string.h>
#include <GLES2/gl2.h>

#define EGL_EGLEXT_PROTOTYPES
#define GL_GLEXT_PROTOTYPES

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2ext.h>
#include <android/native_window_jni.h>

#define LOG_TAG "nativewindow"

#define COLOR_FORMAT_NV21 17
#define COLOR_FORMAT_YV12 0x32315659

JNIEXPORT void JNICALL Java_me_lake_gleslab_MainActivity_ndkdraw
        (JNIEnv *env, jobject thiz, jobject javaSurface, jbyteArray pixelsArray, jint w, jint h,
         jint size) {
    unsigned char *pixels = (unsigned char *) (*env)->GetByteArrayElements(env, pixelsArray, 0);
    ANativeWindow *window = ANativeWindow_fromSurface(env, javaSurface);
    if (window != NULL) {
        ANativeWindow_setBuffersGeometry(window, w, h, COLOR_FORMAT_YV12);
        ANativeWindow_Buffer buffer;
        if (ANativeWindow_lock(window, &buffer, NULL) == 0) {
            if (buffer.width == buffer.stride) {
                memcpy(buffer.bits, pixels, size);
            } else {
                int height = h * 3 / 2;
                int width = w;
                int i = 0;
                for (; i < height; ++i)
                    memcpy(buffer.bits + buffer.stride * i, pixels + width * i, width);
            }
            ANativeWindow_unlockAndPost(window);
        }
        ANativeWindow_release(window);
    }
    (*env)->ReleaseByteArrayElements(env, pixelsArray, pixels, JNI_ABORT);
    return;
}

JNIEXPORT void JNICALL Java_me_lake_gleslab_MainActivity_toYV12
        (JNIEnv *env, jobject thiz, jbyteArray srcarray, jbyteArray dstarray, jint w, jint h) {
    unsigned char *src = (unsigned char *) (*env)->GetByteArrayElements(env, srcarray, 0);
    unsigned char *dst = (unsigned char *) (*env)->GetByteArrayElements(env, dstarray, 0);
    int ySize = w * h;
    int uvSize = ySize >> 1;
    int uSize = uvSize >> 1;
    int i = 0;
    unsigned char *srcycur = src;
    unsigned char *dstycur = dst;
    while (i < ySize) {
        (*dstycur) = (*srcycur);
        ++dstycur;
        srcycur += 4;
        ++i;
    }
    unsigned char *srcucur = src + 1;
    unsigned char *dstvcur = dst + ySize;
    unsigned char *srcvcur = src + 2;
    unsigned char *dstucur = dst + ySize + uSize;
    int y = 0, x = 0;
    for (y = 0; y < h; y++) {
        if (y % 2 == 0)//v
        {
            srcvcur = src + 4 * w * y + 2;
            for (x = 0; x < w; x += 2) {
                (*dstvcur) = (*srcvcur);
                srcvcur += 8;
                ++dstvcur;
            }
        } else//u
        {
            srcucur = src + 4 * w * y + 1;
            for (x = 0; x < w; x += 2) {
                (*dstucur) = (*srcucur);
                srcucur += 8;
                ++dstucur;
            }
        }
    }
    (*env)->ReleaseByteArrayElements(env, srcarray, src, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, dstarray, dst, JNI_ABORT);
    return;
}


JNIEXPORT void JNICALL Java_me_lake_gleslab_MainActivity_readPixel
        (JNIEnv *env, jobject thiz, jbyteArray srcarray, jint f, jint t, jint offset) {
//	unsigned char *src = (unsigned char*)(*env)->GetByteArrayElements(env,srcarray, 0);
//	unsigned char *srcc =src;
    glReadPixels(0, 0, 1280, 720, f, t, offset);
//	unsigned char *po = glMapBufferRange(35051,0,4*921600,1);
//	memcpy(srcc, po, 4*921600);
//	LOGD("po=%u",po);
//	glUnmapBuffer(35051);
//	(*env)->ReleaseByteArrayElements(env,srcarray,src,JNI_ABORT);
}


#define SHADER_VS "\
attribute vec4 aPosition;                               \n\
attribute vec2 aTextureCoord;                           \n\
varying vec2 vTextureCoord;                             \n\
void main(){                                            \n\
    gl_Position= aPosition;                             \n\
    vTextureCoord = aTextureCoord;                      \n\
}"

#define SHADER_FS "\
precision mediump float;                                \n\
varying mediump vec2 vTextureCoord;                     \n\
uniform sampler2D uTexture;                             \n\
void main()                                             \n\
{                                                       \n\
    vec4  color = texture2D(uTexture, vTextureCoord);   \n\
    gl_FragColor = color;                               \n\
}"

void checkError(char *s) {
    int glerr = glGetError();
    int eglerr = eglGetError();
    if(glerr!=GL_NO_ERROR || eglerr!=EGL_SUCCESS){
        LOGE("eglerr=%x,glerr=%x,%s", eglerr, glerr,s);
    } else{
        LOGD("eglerr=%x,glerr=%x,%s", eglerr, glerr,s);
    }
}
int getProgram() {
    int vs = glCreateShader(GL_VERTEX_SHADER);
    int fs = glCreateShader(GL_FRAGMENT_SHADER);
    int vsl = strlen(SHADER_VS);
    int fsl = strlen(SHADER_FS);
    char *v = SHADER_VS;
    char *f = SHADER_FS;
    glShaderSource(vs, 1, &v, &vsl);
    glCompileShader(vs);
    glShaderSource(fs, 1, &f, &fsl);
    glCompileShader(fs);
    checkError("context2.A");
    int program = glCreateProgram();
    checkError("context2.B");
    glAttachShader(program, vs);
    glAttachShader(program, fs);
    checkError("context2.C");
    glLinkProgram(program);
    GLint linkSuccess;
    glGetProgramiv(program, GL_LINK_STATUS, &linkSuccess);
    LOGD("linkSuccess=%d",linkSuccess);
    checkError("context2.D");
    return program;
}

JNIEXPORT jint JNICALL Java_me_lake_gleslab_HomeActivity_runImageKHR
        (JNIEnv *env, jobject thiz, jobject surfaceObject, jint w, jint h) {
    LOGD("runImageKHR.S");
    jclass clazz = (jclass) (*env)->FindClass(env,"me/lake/gleslab/HomeActivity");
    jmethodID loadTextureId = (*env)->GetStaticMethodID(env,clazz, "loadTexture", "()I");

    ANativeWindow *_nativeWindow = ANativeWindow_fromSurface(env, surfaceObject);
    const EGLint attribs[] = {
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT ,
            EGL_BLUE_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_RED_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
//            EGL_DEPTH_SIZE, 16,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_NONE
    };
    EGLDisplay display;
    EGLConfig config;
    EGLint numConfigs;
    EGLint format;
    EGLSurface surface;
    EGLContext context1;
    EGLContext context2;


    if ((display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
        LOGD("getdisplay failed.");
        return -1;
    }
    if (!eglInitialize(display, 0, 0)) {
        LOGD("egl initialize failed.");
        return -1;
    }

    if (!eglChooseConfig(display, attribs, &config, 1, &numConfigs)) {
        LOGD("choose config failed.");
        return -1;
    }

    if (!eglGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_ID, &format)) {
        LOGD("get config failed.");
        return -1;
    }

    ANativeWindow_setBuffersGeometry(_nativeWindow, 0, 0, format);
    if (!(surface = eglCreateWindowSurface(display, config, _nativeWindow, 0))) {
        LOGD("create surface failed.");
        return -1;
    }


    EGLint AttribList[] = {
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL_NONE
    };
    if (!(context1 = eglCreateContext(display, config, 0, AttribList))) {
        LOGD("create context failed.");
        return -1;
    }
    if (!(context2 = eglCreateContext(display, config, 0, AttribList))) {
        LOGD("create context failed.");
        return -1;
    }

    if (!eglMakeCurrent(display, surface, surface, context1)) {
        LOGD("make current failed.");
        return -1;
    }
        short drawIndices[] = {0, 1, 2, 0, 2, 3};
    checkError("init");
    //==========CONTEXT 1======================
    glEnable(GL_TEXTURE_2D);
    GLuint mFboFrame;
    glGenFramebuffers(1, &mFboFrame);
    glBindFramebuffer(GL_FRAMEBUFFER, mFboFrame);

    GLuint tid = 0;
    glGenTextures(1, &tid);
    glBindTexture(GL_TEXTURE_2D, tid);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
    checkError("contextAA");
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                           tid, 0);
    glBindFramebuffer(GL_FRAMEBUFFER, mFboFrame);
    checkError("contextA");
    int program1 = getProgram();
    glUseProgram(program1);
    int mCamTextureLoc1 = glGetUniformLocation(program1, "uTexture");
    int aPostionLocation1 = glGetAttribLocation(program1, "aPosition");
    int aTextureCoordLocation1 = glGetAttribLocation(program1, "aTextureCoord");
    glEnableVertexAttribArray(aPostionLocation1);
    glEnableVertexAttribArray(aTextureCoordLocation1);
    glActiveTexture(GL_TEXTURE0);
    checkError("contextB");
    int tex = (*env)->CallStaticIntMethod(env,clazz, loadTextureId);
    checkError("contextC");
    glBindTexture(GL_TEXTURE_2D,tex);
    glUniform1i(mCamTextureLoc1, 0);
    checkError("contextD");
    float squareVertices1[] = {
            0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 0.0f, 0.0f
    };
    float textureVertices1[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };
    glVertexAttribPointer(aTextureCoordLocation1, 2, GL_FLOAT, GL_FALSE, 2 * 4, textureVertices1);
    glVertexAttribPointer(aPostionLocation1, 3, GL_FLOAT, GL_FALSE, 3 * 4, squareVertices1);
    glViewport(0, 0, w, h);
    glClearColor(0.8f, 0.5f, 0.0f, 0.0f);
    glClear(GL_COLOR_BUFFER_BIT);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, drawIndices);
    glFinish();
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    checkError("context1");
    //KHRImage
    EGLint eglImageAttributes[] = {
            EGL_GL_TEXTURE_LEVEL_KHR, 0,
            EGL_IMAGE_PRESERVED_KHR, EGL_TRUE, EGL_NONE
    };
    EGLClientBuffer egl_buffer = (EGLClientBuffer) (tid);
    EGLImageKHR eglImageHandle = eglCreateImageKHR(
            display, context1, EGL_GL_TEXTURE_2D_KHR, egl_buffer, eglImageAttributes);
    LOGD("eglImageHandle=%x",eglImageHandle);
    checkError("KHRImage");
    //===
    //==========CONTEXT 2======================
    if (!eglMakeCurrent(display, surface, surface, context2)) {
        LOGD("make current failed.");
        return -1;
    }
    int program = getProgram();
    glUseProgram(program);
    checkError("context2.E");
      mCamTextureLoc1 = glGetUniformLocation(program, "uTexture");
     aPostionLocation1 = glGetAttribLocation(program, "aPosition");
     aTextureCoordLocation1 = glGetAttribLocation(program, "aTextureCoord");
    checkError("context2.1.A");
    glEnable(GL_TEXTURE_2D);
    glEnableVertexAttribArray(aPostionLocation1);
    glEnableVertexAttribArray(aTextureCoordLocation1);
    checkError("context2.1.B");
    glActiveTexture(GL_TEXTURE0);
    checkError("context2.1.C");

    GLuint texture;
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glEGLImageTargetTexture2DOES(GL_TEXTURE_2D, eglImageHandle);


    checkError("context2.2");
    glUniform1i(mCamTextureLoc1, 0);
    float squareVertices[] = {
            0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 0.0f, 0.0f
    };
    float textureVertices[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };
    glVertexAttribPointer(aTextureCoordLocation1, 2, GL_FLOAT, GL_FALSE, 2 * 4, textureVertices);
    glVertexAttribPointer(aPostionLocation1, 3, GL_FLOAT, GL_FALSE, 3 * 4, squareVertices);
    glViewport(0, 0, w, h);
    glClearColor(0.0f, 0.0f, 0.8f, 0.0f);
    glClear(GL_COLOR_BUFFER_BIT);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, drawIndices);
    checkError("context2");
    if (!eglSwapBuffers(display, surface)) {
        LOGD("eglSwapBuffers!!!!!");
    }


    LOGD("runImageKHR.E");
}

#endif