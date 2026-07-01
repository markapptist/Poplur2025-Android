package com.apptists.poplur2025.ui;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.view.Surface;
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
public class TransparentGLTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private GLSurfaceView.Renderer renderer;
    private GLThread glThread;
    private int renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY;
    private final long targetFrameNanos;
    private int glesVersion = 2;

    public TransparentGLTextureView(Context context) {
        super(context);
        android.util.Log.d("TransparentGLTextureView", "Constructor called");
        targetFrameNanos = frameIntervalNanos(context);
        setOpaque(false);
        setSurfaceTextureListener(this);
    }

    public void setRenderer(GLSurfaceView.Renderer renderer, int glesVersion) {
        this.renderer = renderer;
        this.glesVersion = glesVersion;
    }

    public void setRenderMode(int renderMode) {
        this.renderMode = renderMode;
        GLThread thread = glThread;
        if (thread != null) thread.setRenderMode(renderMode);
    }

    public void requestRender() {
        GLThread thread = glThread;
        if (thread != null) thread.requestRender();
    }

    public void onPause() {
        GLThread thread = glThread;
        if (thread != null) thread.onPause();
    }

    public void onResume() {
        GLThread thread = glThread;
        if (thread != null) thread.onResume();
    }

    public void onDestroy() {
        GLThread thread = glThread;
        glThread = null;
        if (thread != null) {
            thread.shutdown();
            try {
                // Wait for the thread to finish EGL release to avoid race with new instances.
                // 200ms limit to prevent blocking the main thread if GL is truly stuck.
                thread.join(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (renderer == null) return;
        glThread = new GLThread(surface, width, height, renderer, renderMode, targetFrameNanos, glesVersion);
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
        private final int glesVersion;

        private EGL10 egl;
        private EGLDisplay display;
        private EGLConfig config;
        private EGLContext context;
        private EGLSurface surface;
        private Surface androidSurface;

        GLThread(
                SurfaceTexture surfaceTexture,
                int width,
                int height,
                GLSurfaceView.Renderer renderer,
                int renderMode,
                long targetFrameNanos,
                int glesVersion
        ) {
            super("PoplurCubeTextureGL");
            this.surfaceTexture = surfaceTexture;
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            this.renderer = renderer;
            this.renderMode = renderMode;
            this.targetFrameNanos = Math.max(1L, targetFrameNanos);
            this.glesVersion = glesVersion;
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
                    if (!egl.eglSwapBuffers(display, surface)) {
                        int error = egl.eglGetError();
                        android.util.Log.w("TransparentGLTextureView", "eglSwapBuffers failed: 0x" + Integer.toHexString(error));
                        if (error == EGL10.EGL_BAD_SURFACE || error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                            break;
                        }
                    }
                    if (renderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY) {
                        sleepUntilNextFrame(frameStartNanos);
                    }
                }
            } catch (InterruptedException ignored) {
            } catch (Throwable error) {
                android.util.Log.e("TransparentGLTextureView", "GL render thread failed", error);
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
            if (display == EGL10.EGL_NO_DISPLAY || !egl.eglInitialize(display, version)) {
                throw new RuntimeException("Unable to initialize EGL");
            }

            int requestedGlesVersion = glesVersion >= 3 ? 3 : 2;
            config = chooseConfig(requestedGlesVersion);
            if (config == null && requestedGlesVersion == 3) {
                requestedGlesVersion = 2;
                config = chooseConfig(requestedGlesVersion);
            }
            if (config == null) {
                throw new RuntimeException("No EGL config for transparent GL texture");
            }

            int[] contextAttrs = {0x3098, requestedGlesVersion, EGL10.EGL_NONE};
            context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttrs);
            if ((context == null || context == EGL10.EGL_NO_CONTEXT) && requestedGlesVersion == 3) {
                requestedGlesVersion = 2;
                config = chooseConfig(requestedGlesVersion);
                int[] fallbackAttrs = {0x3098, requestedGlesVersion, EGL10.EGL_NONE};
                context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, fallbackAttrs);
            }
            if (context == null || context == EGL10.EGL_NO_CONTEXT) {
                throw new RuntimeException("Unable to create EGL context: 0x" + Integer.toHexString(egl.eglGetError()));
            }

            androidSurface = new Surface(surfaceTexture);
            surface = egl.eglCreateWindowSurface(display, config, androidSurface, null);
            if (surface == null || surface == EGL10.EGL_NO_SURFACE) {
                throw new RuntimeException("Unable to create EGL window surface: 0x" + Integer.toHexString(egl.eglGetError()));
            }
            if (!egl.eglMakeCurrent(display, surface, surface, context)) {
                throw new RuntimeException("Unable to make EGL context current: 0x" + Integer.toHexString(egl.eglGetError()));
            }
        }

        private EGLConfig chooseConfig(int targetGlesVersion) {
            int renderableType = targetGlesVersion >= 3 ? 0x40 : 4; // EGL_OPENGL_ES3_BIT_KHR or EGL_OPENGL_ES2_BIT
            int[] configSpec = {
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_RENDERABLE_TYPE, renderableType,
                    EGL10.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] count = new int[1];
            if (!egl.eglChooseConfig(display, configSpec, configs, 1, count) || count[0] <= 0) {
                return null;
            }
            return configs[0];
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
            if (androidSurface != null) {
                androidSurface.release();
                androidSurface = null;
            }
            surface = null;
            context = null;
            display = null;
            egl = null;
        }
    }
}
