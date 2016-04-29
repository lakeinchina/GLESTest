package me.lake.gleslab;

import android.content.Context;
import android.opengl.GLES20;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by lake on 16-4-28.
 */
public class ProgramTools {
    private static String readTextFile(Context context, int resId) {
        InputStream inputStream = context.getResources().openRawResource(resId);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder result = new StringBuilder();
        try {
            while ((line = br.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result.toString();
    }

    /**
     * @param context
     * @param vertexShaderResId
     * @param fragmentShaderResId
     * @return program
     */
    public static int createProgram(Context context, int vertexShaderResId, int fragmentShaderResId) {
        String vertexShaderCode = readTextFile(context, vertexShaderResId);
        String fragmentShaderCode = readTextFile(context, fragmentShaderResId);

        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        int[] status = new int[1];
        GLES20.glCompileShader(vertexShader);
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("vertext shader compile,failed:" + GLES20.glGetShaderInfoLog(vertexShader));
        }
        GLES20.glCompileShader(fragmentShader);
        GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("fragment shader compile,failed:" + GLES20.glGetShaderInfoLog(vertexShader));
        }
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("link program,failed:" + GLES20.glGetProgramInfoLog(program));
        }
        return program;
    }
}