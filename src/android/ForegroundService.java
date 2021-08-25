/*
 Copyright 2013 Sebastián Katzer

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package de.appplant.cordova.plugin.background;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.app.NotificationChannel;

import androidx.core.app.NotificationCompat;

import org.json.JSONObject;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

/**
 * Puts the service in a foreground state, where the system considers it to be
 * something the user is actively aware of and thus not a candidate for killing
 * when low on memory.
 */
public class ForegroundService extends Service {

    // Fixed ID for the 'foreground' notification
    public static final int NOTIFICATION_ID = -574543954;

    // Default title of the background notification
    private static final String NOTIFICATION_TITLE =
            "RetailAIM is running";

    // Default text of the background notification
    private static final String NOTIFICATION_TEXT =
            "";

    // Default icon of the background notification
    private static final String NOTIFICATION_ICON = "icon";

    // Binder given to clients
    private final IBinder binder = new ForegroundBinder();

    // Partial wake lock to prevent the app from going to sleep when locked
    private PowerManager.WakeLock wakeLock;

    /**
     * Allow clients to call on to the service.
     */
    @Override
    public IBinder onBind (Intent intent) {
        return binder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class ForegroundBinder extends Binder
    {
        ForegroundService getService()
        {
            // Return this instance of ForegroundService
            // so clients can call public methods
            return ForegroundService.this;
        }
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    @Override
    public void onCreate()
    {
        super.onCreate();

        keepAwake();
    }

    /**
     * No need to run headless on destroy.
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        sleepWell();
    }

    /**
     * Prevent Android from stopping the background service automatically.
     */
    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        //forceForeground();
        return START_STICKY;
    }

    private String CHANNEL_ID = "RetailAIM_Service_BG_MODE";
    public void forceForeground() {
        try{ //Jason
            // API lower than 26 do not need this work around.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        "RetailAIM_Service", NotificationManager.IMPORTANCE_DEFAULT);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("")
                        .setPriority(Notification.PRIORITY_MIN).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setOnlyAlertOnce(true)
                        .setContentText("").build();
                startForeground(1, notification);
                System.out.println("background mode Started Foreground");




                //trick to hide the foreground notification
                Notification notification_;
                NotificationCompat.Builder bBuilder = new NotificationCompat.Builder(
                        this,CHANNEL_ID).setContentTitle("")
                        .setPriority(Notification.PRIORITY_MIN)
                        .setContentText("").setOngoing(true);
                notification_ = bBuilder.build();
                notification_.flags |= Notification.FLAG_FOREGROUND_SERVICE;
                notification_.flags |= Notification.FLAG_NO_CLEAR;
                notification_.flags |= Notification.FLAG_ONGOING_EVENT;
                startForeground(1, notification_);
                stopForeground(true);
                //trick to hide the foreground notification


            }
        }catch(Exception ec){
            System.out.println("background mode forceForeground ec:"+ec.getMessage());
        }
     }
     

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    @SuppressLint("WakelockTimeout")
    private void keepAwake()
    {
        try {
            JSONObject settings = BackgroundMode.getSettings();
            boolean isSilent    = settings.optBoolean("silent", false);

            if (!isSilent) {
               // forceForeground();
                startForeground(NOTIFICATION_ID, makeNotification());
            }

            PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);

            wakeLock = pm.newWakeLock(
                    PARTIAL_WAKE_LOCK, "backgroundmode:wakelock");

            wakeLock.acquire();
        } catch (Exception e) {
            System.out.println("background mode keepAwake ec:"+e.getMessage());
        }


    }

    /**
     * Stop background mode.
     */
    private void sleepWell()
    {
        try {
            stopForeground(true);
            getNotificationManager().cancel(NOTIFICATION_ID);

            if (wakeLock != null) {
                wakeLock.release();
                wakeLock = null;
            }
        } catch (Exception e) {
            System.out.println("background mode sleepWell ec:"+e.getMessage());
        }

    }

    /**
     * Create a notification as the visible part to be able to put the service
     * in a foreground state by using the default settings.
     */
    private Notification makeNotification()
    {
        return makeNotification(BackgroundMode.getSettings());
    }

    /**
     * Create a notification as the visible part to be able to put the service
     * in a foreground state.
     *
     * @param settings The config settings
     */
    private Notification makeNotification (JSONObject settings)
    {

            // use channelid for Oreo and higher
            String CHANNEL_ID = "cordova-plugin-background-mode-id";
            if(Build.VERSION.SDK_INT >= 26){
                // The user-visible name of the channel.
                CharSequence name = CHANNEL_ID;
                // The user-visible description of the channel.
                String description = CHANNEL_ID+" notification";

                int importance = NotificationManager.IMPORTANCE_LOW;

                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name,importance);

                // Configure the notification channel.
                mChannel.setDescription(description);

                getNotificationManager().createNotificationChannel(mChannel);
            }
            String title    = settings.optString("title", NOTIFICATION_TITLE);
            String text     = settings.optString("text", NOTIFICATION_TEXT);
            boolean bigText = settings.optBoolean("bigText", false);

            Context context = getApplicationContext();
            String pkgName  = context.getPackageName();
            Intent intent   = context.getPackageManager()
                    .getLaunchIntentForPackage(pkgName);

            Notification.Builder notification = new Notification.Builder(context)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setOngoing(true)
                    .setSmallIcon(getIconResId(settings));

            if(Build.VERSION.SDK_INT >= 26){
                notification.setChannelId(CHANNEL_ID);
            }

            if (settings.optBoolean("hidden", true)) {
                notification.setPriority(Notification.PRIORITY_MIN);
            }

            if (bigText || text.contains("\n")) {
                notification.setStyle(
                        new Notification.BigTextStyle().bigText(text));
            }

            setColor(notification, settings);

            if (intent != null && settings.optBoolean("resume")) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent contentIntent = PendingIntent.getActivity(
                        context, NOTIFICATION_ID, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);


                notification.setContentIntent(contentIntent);
            }

            return notification.build();


    }

    /**
     * Update the notification.
     *
     * @param settings The config settings
     */
    protected void updateNotification (JSONObject settings)
    {
        try {
            boolean isSilent = settings.optBoolean("silent", false);

            if (isSilent) {
                stopForeground(true);
                return;
            }

            Notification notification = makeNotification(settings);
            getNotificationManager().notify(NOTIFICATION_ID, notification);

        } catch (Exception e) {
            System.out.println("background mode updateNotification ec:"+e.getMessage());
        }

    }

    /**
     * Retrieves the resource ID of the app icon.
     *
     * @param settings A JSON dict containing the icon name.
     */
    private int getIconResId (JSONObject settings)
    {
        String icon = settings.optString("icon", NOTIFICATION_ICON);

        int resId = getIconResId(icon, "mipmap");

        if (resId == 0) {
            resId = getIconResId(icon, "drawable");
        }

        return resId;
    }

    /**
     * Retrieve resource id of the specified icon.
     *
     * @param icon The name of the icon.
     * @param type The resource type where to look for.
     *
     * @return The resource id or 0 if not found.
     */
    private int getIconResId (String icon, String type)
    {
        Resources res  = getResources();
        String pkgName = getPackageName();

        int resId = res.getIdentifier(icon, type, pkgName);

        if (resId == 0) {
            resId = res.getIdentifier("icon", type, pkgName);
        }

        return resId;
    }

    /**
     * Set notification color if its supported by the SDK.
     *
     * @param notification A Notification.Builder instance
     * @param settings A JSON dict containing the color definition (red: FF0000)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setColor (Notification.Builder notification, JSONObject settings)
    {

        String hex = settings.optString("color", null);

        if (Build.VERSION.SDK_INT < 21 || hex == null)
            return;

        try {
            int aRGB = Integer.parseInt(hex, 16) + 0xFF000000;
            notification.setColor(aRGB);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the shared notification service manager.
     */
    private NotificationManager getNotificationManager()
    {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
}
