package com.apptists.poplur2025.ui;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.view.Display;
import android.view.TextureView;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Minimal transparent TextureView EGL host for renderers that implement GLSurfaceView.Renderer.
 * TextureView composes in the normal Android view hierarchy, unlike SurfaceView, so it can sit
 * over Compose without exposing a black compositor plane.
 */
class TransparentGLTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private GLSurfaceView.Renderer renderer;
    private GLThread glThread;
    private int renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY;
    private final long targetFrameNanos;

    TransparentGLTextureView(Context context) {
        super(context);
        targetFrameNanos = frameIntervalNanos(context);
        setOpaque(false);
        setSurfaceTextureListener(this);
    }

    void setRenderer(GLSurfaceView.Renderer renderer) {
        this.renderer = renderer;
    }

    void setRenderMode(int renderMode) {
        this.renderMode = renderMode;
        GLThread thread = glThread;
        if (thread != null) thread.setRenderMode(renderMode);
    }

    void requestRender() {
        GLThread thread = glThread;
        if (thread != null) thread.requestRender();
    }

    void onPause() {
        GLThread thread = glThread;
        if (thread != null) thread.onPause();
    }

    void onResume() {
        GLThread thread = glThread;
        if (thread != null) thread.onResume();
    }

    void onDestroy() {
        GLThread thread = glThread;
        glThread = null;
        if (thread != null) thread.shutdown();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (renderer == null) return;
        glThread = new GLThread(surface, width, height, renderer, renderMode, targetFrameNanos);
        glThread.start();
    }

    private static long frameIntervalNanos(Context context) {
        float refreshRate = 60f;
        Display display = null;
        if (context instanceof android.app.Activity) {
            display = ((android.app.Activity) context).getWindowManager().getDefaultDisplay();
        }
        if (display != null && display.getRefreshRate() > 0f) {
            refreshRate = display.getRefreshRate();
        }
        float targetFps = Math.max(60f, Math.min(refreshRate, 120f));
        return (long) (1_000_000_000L / targetFps);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        GLThread thread = glThread;
        if (thread != null) thread.onResize(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        onDestroy();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    private static final class GLThread extends Thread {
        private final SurfaceTexture surfaceTexture;
        private final GLSurfaceView.Renderer renderer;
        private final Object lock = new Object();
        private volatile boolean shouldExit = false;
        private boolean paused = false;
        private boolean renderRequested = true;
        private boolean sizeChanged = false;
        private final long targetFrameNanos;
        private int width;
        private int height;
        private int renderMode;

        private EGL10 egl;
        private EGLDisplay display;
        private EGLConfig config;
        private EGLContext context;
        private EGLSurface surface;

        GLThread(
                SurfaceTexture surfaceTexture,
                int width,
                int height,
                GLSurfaceView.Renderer renderer,
                int renderMode,
                long targetFrameNanos
        ) {
            super("PoplurCubeTextureGL");
            this.surfaceTexture = surfaceTexture;
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            this.renderer = renderer;
            this.renderMode = renderMode;
            this.targetFrameNanos = Math.max(1L, targetFrameNanos);
        }

        @Override
        public void run() {
            try {
                initEgl();
                GL10 gl = (GL10) context.getGL();
                renderer.onSurfaceCreated(gl, config);
                renderer.onSurfaceChanged(gl, width, height);
                while (!shouldExit) {
                    int w, h;
                    boolean changed;
                    synchronized (lock) {
                        if (paused) {
                            lock.wait(50L);
                            continue;
                        }
                        if (renderMode != GLSurfaceView.RENDERMODE_CONTINUOUSLY && !renderRequested && !sizeChanged) {
                            lock.wait(50L);
                            continue;
                        }
                        changed = sizeChanged;
                        w = width;
                        h = height;
                        sizeChanged = false;
                        renderRequested = false;
                    }
                    long frameStartNanos = System.nanoTime();
                    if (changed) {
                        renderer.onSurfaceChanged(gl, w, h);
                    }
                    renderer.onDrawFrame(gl);
                    egl.eglSwapBuffers(display, surface);
                    if (renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
                        sleepUntilNextFrame(frameStartNanos);
                    }
                }
            } catch (InterruptedException ignored) {
            } finally {
                releaseEgl();
            }
        }

        void onResize(int width, int height) {
            synchronized (lock) {
                this.width = Math.max(1, width);
                this.height = Math.max(1, height);
                sizeChanged = true;
                renderRequested = true;
                lock.notifyAll();
            }
        }

        void setRenderMode(int renderMode) {
            synchronized (lock) {
                this.renderMode = renderMode;
                renderRequested = true;
                lock.notifyAll();
            }
        }

        void requestRender() {
            synchronized (lock) {
                renderRequested = true;
                lock.notifyAll();
            }
        }

        void onPause() {
            synchronized (lock) {
                paused = true;
                lock.notifyAll();
            }
        }

        void onResume() {
            synchronized (lock) {
                paused = false;
                renderRequested = true;
                lock.notifyAll();
            }
        }

        void shutdown() {
            synchronized (lock) {
                shouldExit = true;
                lock.notifyAll();
            }
            interrupt();
        }

        private void sleepUntilNextFrame(long frameStartNanos) throws InterruptedException {
            long elapsedNanos = System.nanoTime() - frameStartNanos;
            long remainingNanos = targetFrameNanos - elapsedNanos;
            if (remainingNanos <= 0L) return;
            long millis = remainingNanos / 1_000_000L;
            int nanos = (int) (remainingNanos % 1_000_000L);
            Thread.sleep(millis, nanos);
        }

        private void initEgl() {
            egl = (EGL10) EGLContext.getEGL();
            display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            egl.eglInitialize(display, version);

            int[] configSpec = {
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_RENDERABLE_TYPE, 4,
                    EGL10.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] count = new int[1];
            egl.eglChooseConfig(display, configSpec, configs, 1, count);
            config = configs[0];

            int[] contextAttrs = {0x3098, 2, EGL10.EGL_NONE};
            context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttrs);
            surface = egl.eglCreateWindowSurface(display, config, surfaceTexture, null);
            egl.eglMakeCurrent(display, surface, surface, context);
        }

        private void releaseEgl() {
            if (egl == null) return;
            if (display != null && display != EGL10.EGL_NO_DISPLAY) {
                egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                if (surface != null && surface != EGL10.EGL_NO_SURFACE) {
                    egl.eglDestroySurface(display, surface);
                }
                if (context != null && context != EGL10.EGL_NO_CONTEXT) {
                    egl.eglDestroyContext(display, context);
                }
                egl.eglTerminate(display);
            }
        }
    }
}
