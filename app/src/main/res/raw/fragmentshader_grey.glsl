varying lowp vec2 vTextureCoord;
uniform sampler2D samplerY;
uniform sampler2D samplerU;
uniform sampler2D samplerV;

const float PI = 3.1415926535;
const float aperture = 180.0;
const float apertureHalf = 0.5 * aperture * (PI / 180.0);
const float maxFactor = sin(apertureHalf);

void main()
{
    mediump vec3 yuv;
    lowp vec3 rgb;
      vec2 pos = 2.0 * vTextureCoord.st - 1.0;
      float l = length(pos);

      if (l > 1.0) {
        gl_FragColor = vec4(0, 0, 0, 1);
      }
      else {
        float x = maxFactor * pos.x;
        float y = maxFactor * pos.y;
        float n = length(vec2(x, y));
        float z = sqrt(1.0 - n * n);
        float r = atan(n, z) / PI;
        float phi = atan(y, x);
        float u = r * cos(phi) + 0.5;
        float v = r * sin(phi) + 0.5;
        yuv.x = texture2D(samplerY,vec2(u,v)).r;
        yuv.y = texture2D(samplerU,vec2(u,v)).r - 0.5;
        yuv.z = texture2D(samplerV,vec2(u,v)).r - 0.5;
        rgb = mat3(1,1,1,0,-0.39465,2.03211,1.13983,-0.5806,0)*yuv;
        rgb.r*=1.5;
        gl_FragColor = vec4(rgb,1);
    }
}