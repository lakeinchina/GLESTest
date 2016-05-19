attribute vec4 aPosition;
attribute vec2 aTextureCoord;
varying vec2 vTextureCoord;

void main(){
    gl_Position= aPosition;
    vTextureCoord = vec2(aTextureCoord.x,aTextureCoord.y);
}