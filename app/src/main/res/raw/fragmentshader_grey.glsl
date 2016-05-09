varying lowp vec2 vTextureCoord;
uniform sampler2D samplerY;
uniform sampler2D samplerU;
uniform sampler2D samplerV;

const mediump float PI = 3.1415926535;
const mediump float aperture = 180.0;
const mediump float apertureHalf = 0.5 * aperture * (PI / 180.0);
const mediump float maxFactor = sin(apertureHalf);

void main()
{
    mediump vec3 yuv;
    lowp vec3 rgb;
     mediump vec2 pos = 2.0 * vTextureCoord.st - 1.0;
     mediump float l = length(pos);

      if (l > 1.0) {
        gl_FragColor = vec4(0.1,0.2,0.5,1);
      }
      else {
        mediump float x = maxFactor * pos.x;
        mediump float y = maxFactor * pos.y;
        mediump float n = length(vec2(x, y));
        mediump float z = sqrt(1.0 - n * n);
        mediump float r = atan(n, z) / PI;
        mediump float phi = atan(y, x);
        mediump float u = r * cos(phi) + 0.5;
        mediump float v = r * sin(phi) + 0.5;
        yuv.x = texture2D(samplerY,vec2(u,v)).r;
        yuv.y = texture2D(samplerU,vec2(u,v)).r;
        yuv.z = texture2D(samplerV,vec2(u,v)).r;
        gl_FragColor = vec4(yuv,1);
    }
}