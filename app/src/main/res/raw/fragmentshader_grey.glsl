#extension GL_OES_EGL_image_external : require
precision mediump float;

varying mediump vec2 vTextureCoord;
uniform samplerExternalOES uTexture;

void main()
{
    vec4  color = texture2D(uTexture, vTextureCoord);
    color.g*=1.5;
    gl_FragColor = color;
}