package com.apptists.poplur2025.map;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Uses only platform APIs so release optimization of app libraries cannot
 * remove methods needed by the test runner. No production test hooks. */
public final class MapRecoveryInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            runScenario(this);
            result.putString("stream", "\nOK (1 test)\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", "\nFAILURE: " + trace(failure) + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    public static void runScenario(Instrumentation instrumentation) throws Exception {
        Context context = instrumentation.getTargetContext();
        File evidence = new File(context.getExternalFilesDir(null), "map-recovery-evidence");
        evidence.mkdirs();
        for (String name : new String[]{"android-flat-map.png", "android-clay-retry.png", "android-recovered-map.png", "failure.png", "failure.txt"}) {
            new File(evidence, name).delete();
        }
        Activity activity = instrumentation.startActivitySync(new Intent()
            .setClassName(context, "com.apptists.poplur2025.PostLoginWebActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("fromAuth", true));
        try {
            WebView[] web = new WebView[1];
            instrumentation.runOnMainSync(() -> {
                web[0] = findWebView(activity.getWindow().getDecorView());
                check(web[0] != null && web[0].getUrl() != null, "Missing home WebView");
                web[0].loadUrl(web[0].getUrl().replace("view=home", "view=map"));
            });
            Scenario scenario = new Scenario(instrumentation, web[0]);
            String frame = "document.querySelector('iframe[data-poplur-screen=map]')";
            String ready = "(()=>{const w=" + frame + "?.contentWindow;return !!(w?.__poplurMap?.nativeRenderer && w.__poplurMap.loaded())})()";
            String loading = "document.querySelector('[data-map-loading]')";
            scenario.waitFor("native map and visible map surface", "!" + loading + " && " + ready);
            // The actual native world-view screenshot distinguishes a flat map
            // from the SDK globe; normal shipping camera defaults stay intact.
            scenario.js(frame + ".contentWindow.__poplurMap.jumpTo({center:[0,15],zoom:1,pitch:0,bearing:0});true");
            SystemClock.sleep(3000);
            screenshot(instrumentation, evidence, "android-flat-map.png");
            // Keep the map attended and close automatic post previews during
            // fault injection. An intercepted post otherwise covers the map,
            // correctly suspending its retry lifecycle before we can test it.
            scenario.js("window.__mapRecoveryQuiesce=()=>{const f=" + frame + ";f?.contentWindow?.postMessage({type:'poplur-map-front',front:true},'*');for(const d of [document,f?.contentDocument]){if(!d)continue;for(const card of d.querySelectorAll('[data-poplur-dialog]')){if([...card.querySelectorAll('button')].some(b=>b.textContent.trim().startsWith('See in Feed')))card.click()}}};window.__mapRecoveryQuiesce();window.__mapRecoveryTimer=setInterval(window.__mapRecoveryQuiesce,250);true");
            // Start real tile traffic before teardown to exercise TLS cleanup.
            scenario.js(frame + ".contentWindow.__poplurMap.jumpTo({center:[-79.389,43.6418],zoom:16,pitch:45});true");
            SystemClock.sleep(5000);
            scenario.waitFor("map after tile traffic", "!" + loading + " && " + ready);
            scenario.js("window.__mapRecoveryBlock=e=>{if(['poplur-map-ready','poplur-map-loading'].includes(e.data?.type))e.stopImmediatePropagation()};window.addEventListener('message',window.__mapRecoveryBlock,true);true");
            String dialog = "document.querySelector('[data-poplur-dialog=map-unavailable]')";
            for (int attempt = 0; attempt < 8; attempt++) {
                if ("true".equals(scenario.js("!!" + dialog))) break;
                scenario.js("(()=>{const f=" + frame + ";if(!f?.contentWindow)return false;window.dispatchEvent(new MessageEvent('message',{source:f.contentWindow,data:{type:'poplur-map-failed'}}));return true})()");
                SystemClock.sleep(2000);
            }
            scenario.waitFor("clay recovery dialog", "!!" + dialog);
            check("false".equals(scenario.js("!!" + loading)), "Loading animation remains behind failure");
            screenshot(instrumentation, evidence, "android-clay-retry.png");
            scenario.js("window.removeEventListener('message',window.__mapRecoveryBlock,true);[..." + dialog + ".querySelectorAll('button')].find(b=>b.textContent.trim().startsWith('Try again')).click();true");
            scenario.waitFor("retry removes dialog and restores native map", "!" + dialog + " && !" + loading + " && " + ready);
            SystemClock.sleep(2000);
            screenshot(instrumentation, evidence, "android-recovered-map.png");
            scenario.js("clearInterval(window.__mapRecoveryTimer);delete window.__mapRecoveryTimer;delete window.__mapRecoveryQuiesce;true");
        } catch (Throwable failure) {
            try (PrintWriter writer = new PrintWriter(new File(evidence, "failure.txt"))) { writer.print(trace(failure)); }
            screenshot(instrumentation, evidence, "failure.png");
            throw failure;
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }

    private static final class Scenario {
        private final Instrumentation instrumentation;
        private final WebView web;
        Scenario(Instrumentation instrumentation, WebView web) { this.instrumentation = instrumentation; this.web = web; }
        String js(String expression) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            String[] result = {"null"};
            instrumentation.runOnMainSync(() -> web.evaluateJavascript(expression, value -> { result[0] = value; latch.countDown(); }));
            return latch.await(3, TimeUnit.SECONDS) ? result[0] : "null";
        }
        void waitFor(String label, String condition) throws InterruptedException {
            long deadline = SystemClock.uptimeMillis() + 90000;
            while (!"true".equals(js(condition))) {
                check(SystemClock.uptimeMillis() < deadline, "Timed out: " + label);
                SystemClock.sleep(500);
            }
        }
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    private static String trace(Throwable failure) {
        StringWriter text = new StringWriter(); failure.printStackTrace(new PrintWriter(text)); return text.toString();
    }
    private static WebView findWebView(View view) {
        if (view instanceof WebView) return (WebView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                WebView found = findWebView(group.getChildAt(index));
                if (found != null) return found;
            }
        }
        return null;
    }
    private static void screenshot(Instrumentation instrumentation, File directory, String name) throws Exception {
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        check(bitmap != null, "Screenshot unavailable");
        try (FileOutputStream output = new FileOutputStream(new File(directory, name))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); }
        finally { bitmap.recycle(); }
    }
}
