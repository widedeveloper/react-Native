/*
 * PSPDFKitModule.java
 *
 *   PSPDFKit
 *
 *   Copyright © 2017-2018 PSPDFKit GmbH. All rights reserved.
 *
 *   THIS SOURCE CODE AND ANY ACCOMPANYING DOCUMENTATION ARE PROTECTED BY INTERNATIONAL COPYRIGHT LAW
 *   AND MAY NOT BE RESOLD OR REDISTRIBUTED. USAGE IS BOUND TO THE PSPDFKIT LICENSE AGREEMENT.
 *   UNAUTHORIZED REPRODUCTION OR DISTRIBUTION IS SUBJECT TO CIVIL AND CRIMINAL PENALTIES.
 *   This notice may not be removed from this file.
 */

package com.pspdfkit.react;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.pspdfkit.PSPDFKit;
import com.pspdfkit.document.PdfDocument;
import com.pspdfkit.listeners.SimpleDocumentListener;
import com.pspdfkit.ui.PdfActivity;

import java.util.HashMap;
import java.util.Map;

public class PSPDFKitModule extends ReactContextBaseJavaModule implements Application.ActivityLifecycleCallbacks {

    private static final String VERSION_KEY = "versionString";
    private static final String FILE_SCHEME = "file:///";

    @Nullable
    private Activity resumedActivity;
    @Nullable
    private Runnable onPdfActivityOpenedTask;

    public PSPDFKitModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "PSPDFKit";
    }

    @ReactMethod
    public void present(@NonNull String document, @NonNull ReadableMap configuration) {
        if (getCurrentActivity() != null) {
            if (resumedActivity == null) {
                // We register an activity lifecycle callback so we can get notified of the current activity.
                getCurrentActivity().getApplication().registerActivityLifecycleCallbacks(this);
            }
            ConfigurationAdapter configurationAdapter = new ConfigurationAdapter(getCurrentActivity(), configuration);
            // This is an edge case where file scheme is missing.
            if (Uri.parse(document).getScheme() == null) {
                document = FILE_SCHEME + document;
            }

            PdfActivity.showDocument(getCurrentActivity(), Uri.parse(document), configurationAdapter.build());
        }
    }

    @ReactMethod
    public void presentImage(@NonNull String imageDocument, @NonNull ReadableMap configuration) {
        if (getCurrentActivity() != null) {
            if (resumedActivity == null) {
                // We register an activity lifecycle callback so we can get notified of the current activity.
                getCurrentActivity().getApplication().registerActivityLifecycleCallbacks(this);
            }
            ConfigurationAdapter configurationAdapter = new ConfigurationAdapter(getCurrentActivity(), configuration);
            // This is an edge case where file scheme is missing.
            if (Uri.parse(imageDocument).getScheme() == null) {
                imageDocument = FILE_SCHEME + imageDocument;
            }

            PdfActivity.showImage(getCurrentActivity(), Uri.parse(imageDocument), configurationAdapter.build());
        }
    }

    @ReactMethod
    public synchronized void setPageIndex(final int pageIndex, final boolean animated) {
        if (resumedActivity instanceof PdfActivity) {
            final PdfActivity activity = (PdfActivity) resumedActivity;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (activity.getDocument() != null) {
                        // If the document is loaded we can instantly set the page index.
                        activity.setPageIndex(pageIndex, animated);
                    } else {
                        activity.getPdfFragment().addDocumentListener(new SimpleDocumentListener() {
                            @Override
                            public void onDocumentLoaded(@NonNull PdfDocument document) {
                                // Once the document is loaded set the page index.
                                activity.setPageIndex(pageIndex, animated);
                                activity.getPdfFragment().removeDocumentListener(this);
                            }
                        });
                    }
                }
            });
        } else {
            // Queue up a runnable to set the page index as soon as a PdfActivity is available.
            onPdfActivityOpenedTask = new Runnable() {
                @Override
                public void run() {
                    setPageIndex(pageIndex, animated);
                }
            };
        }
    }

    @NonNull
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put(VERSION_KEY, PSPDFKit.VERSION);
        return constants;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        resumedActivity = activity;
        if (resumedActivity instanceof PdfActivity && onPdfActivityOpenedTask != null) {
            // Run our queued up task when a PdfActivity is displayed.
            onPdfActivityOpenedTask.run();
            onPdfActivityOpenedTask = null;
        }
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        if (activity == resumedActivity) {
            resumedActivity = null;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public synchronized void onActivityDestroyed(Activity activity) {
        if (activity == resumedActivity) {
            resumedActivity = null;
        }
    }
}
