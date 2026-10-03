package object.p2pwificam.clientActivity;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRender implements GLSurfaceView.Renderer {
    final float[] positionBufferData;
    final float[] textCoodBufferData;
    int mHeight = 0;
    ByteBuffer mUByteBuffer = null;
    ByteBuffer mVByteBuffer = null;
    int mWidth = 0;
    ByteBuffer mYByteBuffer = null;
    FloatBuffer positionBuffer = null;
    int positionSlot = 0;
    int programHandle = 0;
    int texRangeSlot = 0;
    int[] texture = new int[3];
    int[] textureSlot = new int[3];
    int vertexShader = 0;
    int yuvFragmentShader = 0;
    FloatBuffer textCoodBuffer = null;

    public MyRender(GLSurfaceView paramGLSurfaceView) {
        float[] arrayOfFloat1 = {0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f};
        this.textCoodBufferData = arrayOfFloat1;
        float[] arrayOfFloat = {-1.0f, 1.0f, 0.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, -1.0f, 0.0f, 1.0f};
        this.positionBufferData = arrayOfFloat;
        paramGLSurfaceView.setEGLContextClientVersion(2);
    }

    public static int compileShader(String paramString, int paramInt) {
        int i = GLES20.glCreateShader(paramInt);
        if (i != 0) {
            int[] arrayOfInt = new int[1];
            GLES20.glShaderSource(i, paramString);
            GLES20.glCompileShader(i);
            GLES20.glGetShaderiv(i, 35713, arrayOfInt, 0);
            if (arrayOfInt[0] == 0) {
                Log.e("compileShader", "compile shader err:" + GLES20.glGetProgramInfoLog(i));
                GLES20.glDeleteShader(i);
                return 0;
            }
            return i;
        }
        return i;
    }

    public long createShaders() {
        String fragmentShaderCode = "uniform sampler2D Ytex;\n" +
                "uniform sampler2D Utex;\n" +
                "uniform sampler2D Vtex;\n" +
                "precision mediump float;\n" +
                "varying vec4 VaryingTexCoord0;\n" +
                "vec4 color;\n" +
                "void main()\n" +
                "{\n" +
                "float yuv0 = (texture2D(Ytex,VaryingTexCoord0.xy)).r;\n" +
                "float yuv1 = (texture2D(Utex,VaryingTexCoord0.xy)).r;\n" +
                "float yuv2 = (texture2D(Vtex,VaryingTexCoord0.xy)).r;\n" +
                "color.r = yuv0 + 1.4022 * yuv2 - 0.7011;\n" +
                "color.r = (color.r < 0.0) ? 0.0 : ((color.r > 1.0) ? 1.0 : color.r);\n" +
                "color.g = yuv0 - 0.3456 * yuv1 - 0.7145 * yuv2 + 0.53005;\n" +
                "color.g = (color.g < 0.0) ? 0.0 : ((color.g > 1.0) ? 1.0 : color.g);\n" +
                "color.b = yuv0 + 1.771 * yuv1 - 0.8855;\n" +
                "color.b = (color.b < 0.0) ? 0.0 : ((color.b > 1.0) ? 1.0 : color.b);\n" +
                "gl_FragColor = color;\n" +
                "}\n";

        String vertexShaderCode = "uniform mat4 uMVPMatrix;\n" +
                "attribute vec4 vPosition;\n" +
                "attribute vec4 myTexCoord;\n" +
                "varying vec4 VaryingTexCoord0;\n" +
                "void main(){\n" +
                "VaryingTexCoord0 = myTexCoord;\n" +
                "gl_Position = vPosition;\n" +
                "}\n";

        int[] arrayOfInt = new int[1];
        this.vertexShader = compileShader(vertexShaderCode, 35633);
        this.yuvFragmentShader = compileShader(fragmentShaderCode, 35632);
        this.programHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(this.programHandle, this.vertexShader);
        GLES20.glAttachShader(this.programHandle, this.yuvFragmentShader);
        GLES20.glLinkProgram(this.programHandle);
        GLES20.glGetProgramiv(this.programHandle, 35714, arrayOfInt, 0);
        if (arrayOfInt[0] == 0) {
            Log.e("createShaders", "link program err:" + GLES20.glGetProgramInfoLog(this.programHandle));
            destroyShaders();
        }
        this.texRangeSlot = GLES20.glGetAttribLocation(this.programHandle, "myTexCoord");
        this.textureSlot[0] = GLES20.glGetUniformLocation(this.programHandle, "Ytex");
        this.textureSlot[1] = GLES20.glGetUniformLocation(this.programHandle, "Utex");
        this.textureSlot[2] = GLES20.glGetUniformLocation(this.programHandle, "Vtex");
        this.positionSlot = GLES20.glGetAttribLocation(this.programHandle, "vPosition");
        return 0L;
    }

    public synchronized long destroyShaders() {
        if (this.programHandle != 0) {
            try {
                GLES20.glDetachShader(this.programHandle, this.yuvFragmentShader);
                GLES20.glDetachShader(this.programHandle, this.vertexShader);
                GLES20.glDeleteProgram(this.programHandle);
            } catch (Exception ignored) {}
            this.programHandle = 0;
        }
        if (this.yuvFragmentShader != 0) {
            try {
                GLES20.glDeleteShader(this.yuvFragmentShader);
            } catch (Exception ignored) {}
            this.yuvFragmentShader = 0;
        }
        if (this.vertexShader != 0) {
            try {
                GLES20.glDeleteShader(this.vertexShader);
            } catch (Exception ignored) {}
            this.vertexShader = 0;
        }
        return 0L;
    }

    public int draw(ByteBuffer paramByteBuffer1, ByteBuffer paramByteBuffer2, ByteBuffer paramByteBuffer3, int paramInt1, int paramInt2) {
        GLES20.glClear(16384);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glUseProgram(this.programHandle);
        paramByteBuffer1.position(0);
        GLES20.glActiveTexture(33984);
        loadTexture(this.texture[0], paramInt1, paramInt2, paramByteBuffer1);
        paramByteBuffer2.position(0);
        GLES20.glActiveTexture(33985);
        loadTexture(this.texture[1], paramInt1 >> 1, paramInt2 >> 1, paramByteBuffer2);
        paramByteBuffer3.position(0);
        GLES20.glActiveTexture(33986);
        loadTexture(this.texture[2], paramInt1 >> 1, paramInt2 >> 1, paramByteBuffer3);
        GLES20.glUniform1i(this.textureSlot[0], 0);
        GLES20.glUniform1i(this.textureSlot[1], 1);
        GLES20.glUniform1i(this.textureSlot[2], 2);
        this.positionBuffer.position(0);
        GLES20.glEnableVertexAttribArray(this.positionSlot);
        GLES20.glVertexAttribPointer(this.positionSlot, 4, 5126, false, 0, (Buffer) this.positionBuffer);
        this.textCoodBuffer.position(0);
        GLES20.glEnableVertexAttribArray(this.texRangeSlot);
        GLES20.glVertexAttribPointer(this.texRangeSlot, 4, 5126, false, 0, (Buffer) this.textCoodBuffer);
        GLES20.glDrawArrays(5, 0, 4);
        GLES20.glDisableVertexAttribArray(this.positionSlot);
        GLES20.glDisableVertexAttribArray(this.texRangeSlot);
        return 0;
    }

    public int loadTexture(int paramInt1, int paramInt2, int paramInt3, Buffer paramBuffer) {
        GLES20.glBindTexture(3553, paramInt1);
        GLES20.glTexParameteri(3553, 10241, 9729);
        GLES20.glTexParameteri(3553, 10240, 9729);
        GLES20.glTexParameteri(3553, 10242, 33071);
        GLES20.glTexParameteri(3553, 10243, 33071);
        GLES20.glTexImage2D(3553, 0, 6409, paramInt2, paramInt3, 0, 6409, 5121, paramBuffer);
        return 0;
    }

    public int loadVBOs() {
        this.textCoodBuffer = ByteBuffer.allocateDirect(this.textCoodBufferData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.textCoodBuffer.put(this.textCoodBufferData).position(0);
        this.positionBuffer = ByteBuffer.allocateDirect(this.positionBufferData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.positionBuffer.put(this.positionBufferData).position(0);
        return 0;
    }

    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;

    @Override
    public void onDrawFrame(GL10 paramGL10) {
        GLES20.glClear(16384);
        synchronized (this) {
            if (this.mWidth != 0 && this.mHeight != 0 && this.mYByteBuffer != null && this.mUByteBuffer != null && this.mVByteBuffer != null) {
                // 自动根据视频实际宽高比（如 16:9）计算 Viewport，保证画面永不变形、不被竖向拉伸
                if (mSurfaceWidth > 0 && mSurfaceHeight > 0) {
                    float videoAspect = (float) this.mWidth / (float) this.mHeight;
                    float surfaceAspect = (float) mSurfaceWidth / (float) mSurfaceHeight;
                    int vpX = 0, vpY = 0, vpW = mSurfaceWidth, vpH = mSurfaceHeight;
                    if (surfaceAspect > videoAspect) {
                        // 容器较宽，左右留黑边 (Pillarbox)
                        vpW = (int) (mSurfaceHeight * videoAspect);
                        vpX = (mSurfaceWidth - vpW) / 2;
                    } else {
                        // 容器较高，上下留黑边 (Letterbox)
                        vpH = (int) (mSurfaceWidth / videoAspect);
                        vpY = (mSurfaceHeight - vpH) / 2;
                    }
                    GLES20.glViewport(vpX, vpY, vpW, vpH);
                }
                draw(this.mYByteBuffer, this.mUByteBuffer, this.mVByteBuffer, this.mWidth, this.mHeight);
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 paramGL10, int width, int height) {
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 paramGL10, EGLConfig paramEGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glGenTextures(3, this.texture, 0);
        createShaders();
        loadVBOs();
    }

    public int writeSample(byte[] paramArrayOfByte, int width, int height) {
        synchronized (this) {
            if (width == 0 || height == 0 || paramArrayOfByte == null) {
                return 0;
            }
            if (width != this.mWidth || height != this.mHeight) {
                this.mWidth = width;
                this.mHeight = height;
                this.mYByteBuffer = ByteBuffer.allocateDirect(this.mWidth * this.mHeight);
                this.mUByteBuffer = ByteBuffer.allocateDirect((this.mWidth * this.mHeight) / 4);
                this.mVByteBuffer = ByteBuffer.allocateDirect((this.mWidth * this.mHeight) / 4);
            }
            if (this.mYByteBuffer != null) {
                this.mYByteBuffer.position(0);
                this.mYByteBuffer.put(paramArrayOfByte, 0, this.mWidth * this.mHeight);
                this.mYByteBuffer.position(0);
            }
            if (this.mUByteBuffer != null) {
                this.mUByteBuffer.position(0);
                this.mUByteBuffer.put(paramArrayOfByte, this.mWidth * this.mHeight, (this.mWidth * this.mHeight) / 4);
                this.mUByteBuffer.position(0);
            }
            if (this.mVByteBuffer != null) {
                this.mVByteBuffer.position(0);
                this.mVByteBuffer.put(paramArrayOfByte, ((this.mWidth * this.mHeight) * 5) / 4, (this.mWidth * this.mHeight) / 4);
                this.mVByteBuffer.position(0);
            }
            return 1;
        }
    }
}
