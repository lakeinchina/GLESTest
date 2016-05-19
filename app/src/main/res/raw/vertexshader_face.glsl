#define SAMPLES 9

attribute vec4 aPosition;
attribute vec2 aTextureCoord;

varying vec2 vTextureCoord;
varying vec2 vBlurTextureCoord[SAMPLES];

uniform float uTexelWidthOffset;
uniform float uTexelHeightOffset;

void main(){
    gl_Position= aPosition;
    vTextureCoord = aTextureCoord;
    int multiplier = 0;
    vec2 blurStep;
    vec2 offset = vec2(uTexelHeightOffset, uTexelWidthOffset);

    for (int i = 0; i < SAMPLES; i++)
    {
        multiplier = (i - ((SAMPLES-1) / 2));
        // ToneCurve in x (horizontal)
        blurStep = float(multiplier) * offset;
        vBlurTextureCoord[i] = vTextureCoord + blurStep;
    }
}