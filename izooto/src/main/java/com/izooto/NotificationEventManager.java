package com.izooto;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Random;

public class NotificationEventManager {

    private static Bitmap notificationIcon, notificationBanner;

    public static void manageNotification(Payload payload) {
        if (payload.getFetchURL() == null || payload.getFetchURL().isEmpty())
            showNotification(payload);
        else
            processPayload(payload);
    }

    private static void processPayload(final Payload payload) {
        RestClient.get(payload.getFetchURL(), new RestClient.ResponseHandler() {
            @Override
            void onSuccess(String response) {
                super.onSuccess(response);
                if (response != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        parseJson(payload, jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            void onFailure(int statusCode, String response, Throwable throwable) {
                super.onFailure(statusCode, response, throwable);

            }
        });
    }

    private static void parseJson(Payload payload, JSONObject jsonObject) {
        try {
            payload.setLink(getParsedvalue(jsonObject, payload.getLink()));
            payload.setTitle(getParsedvalue(jsonObject, payload.getTitle()));
            payload.setMessage(getParsedvalue(jsonObject, payload.getMessage()));
            payload.setIcon(getParsedvalue(jsonObject, payload.getIcon()));
            payload.setBanner(getParsedvalue(jsonObject, payload.getBanner()));
            payload.setAct1name(getParsedvalue(jsonObject, payload.getAct1name()));
            payload.setAct1link(getParsedvalue(jsonObject, payload.getAct1link()));
            payload.setAct2name(getParsedvalue(jsonObject, payload.getAct2name()));
            payload.setAct2link(getParsedvalue(jsonObject, payload.getAct2link()));
            showNotification(payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String getParsedvalue(JSONObject jsonObject, String sourceString) {
        try {
            if (sourceString.startsWith("~"))
                return sourceString.replace("~", "");
            else {
                if (sourceString.contains(".")) {
                    JSONObject jsonObject1 = null;
                    String[] linkArray = sourceString.split("\\.");
                    for (int i = 0; i < linkArray.length; i++) {
                        if (linkArray[i].contains("[")) {
                            String[] linkArray1 = linkArray[i].split("\\[");
                            if (jsonObject1 == null)
                                jsonObject1 = jsonObject.getJSONArray(linkArray1[0]).getJSONObject(Integer.parseInt(linkArray1[1].replace("]", "")));
                            else
                                jsonObject1 = jsonObject1.getJSONArray(linkArray1[0]).getJSONObject(Integer.parseInt(linkArray1[1].replace("]", "")));

                        } else {
                            return jsonObject1.optString(linkArray[i]);
                        }

                    }
                } else
                    return jsonObject.getString(sourceString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static void showNotification(final Payload payload) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable notificationRunnable = new Runnable() {
            @Override
            public void run() {

                String link = payload.getLink();
                String link1 = payload.getAct1link();
                String link2 = payload.getAct2link();
                if (payload.getFetchURL() == null || payload.getFetchURL().isEmpty()) {
                    if (link.contains("{BROWSERKEYID}"))
                        link = link.replace("{BROWSERKEYID}", PreferenceUtil.getInstance(iZooto.appContext).getStringData(AppConstant.FCM_DEVICE_TOKEN));
                    if (link1.contains("{BROWSERKEYID}"))
                        link1 = link1.replace("{BROWSERKEYID}", PreferenceUtil.getInstance(iZooto.appContext).getStringData(AppConstant.FCM_DEVICE_TOKEN));
                    if (link2.contains("{BROWSERKEYID}"))
                        link2 = link2.replace("{BROWSERKEYID}", PreferenceUtil.getInstance(iZooto.appContext).getStringData(AppConstant.FCM_DEVICE_TOKEN));
                } else {
                    link = getFinalUrl(payload);
                }
                String channelId = iZooto.appContext.getString(R.string.default_notification_channel_id);
                NotificationCompat.Builder notificationBuilder = null;
                Intent intent = null;
                if (payload.getInapp() == 1)
                intent = WebViewActivity.createIntent(iZooto.appContext, link);
                else
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                PendingIntent pendingIntent = PendingIntent.getActivity(iZooto.appContext, new Random().nextInt(100) /* Request code */, intent,
                        PendingIntent.FLAG_ONE_SHOT);
                notificationBuilder = new NotificationCompat.Builder(iZooto.appContext, channelId)
                        .setContentTitle(payload.getTitle())
                        .setSmallIcon(R.drawable.irctc_icon)
                        .setContentText(payload.getMessage())
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(payload.getMessage()))
                        .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND).setVibrate(new long[]{1000, 1000})
                        .setSound(defaultSoundUri)
                        .setAutoCancel(true);
                if (notificationIcon != null)
                    notificationBuilder.setLargeIcon(notificationIcon);
                else if (notificationBanner != null)
                    notificationBuilder.setLargeIcon(notificationBanner);
                if (notificationBanner != null)
                    notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(notificationBanner)
                            .bigLargeIcon(notificationIcon).setSummaryText(payload.getMessage()));
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
//                    notificationBuilder.setColor(ContextCompat.getColor(iZooto.appContext, R.color.colorPrimary));

                NotificationManager notificationManager =
                        (NotificationManager) iZooto.appContext.getSystemService(Context.NOTIFICATION_SERVICE);
                int notificaitionId = (int) System.currentTimeMillis();
                if (payload.getAct1name() != null && !payload
                        .getAct1name().isEmpty()) {
                    Intent btn1 = new Intent(iZooto.appContext, NotificationActionReceiver.class);
//                    btn1.setAction(AppConstant.ACTION_BTN_ONE);
                    btn1.putExtra(AppConstant.KEY_WEB_URL, link1);
                    btn1.putExtra(AppConstant.KEY_NOTIFICITON_ID, notificaitionId);
                    btn1.putExtra(AppConstant.KEY_IN_APP, payload.getInapp());
                    PendingIntent pendingIntent1 = PendingIntent.getBroadcast(iZooto.appContext, new Random().nextInt(100), btn1, PendingIntent.FLAG_UPDATE_CURRENT);
                    NotificationCompat.Action action1 =
                            new NotificationCompat.Action.Builder(
                                    0, payload.getAct1name(), pendingIntent1
                            ).build();
                    notificationBuilder.addAction(action1);
                }

                if (payload.getAct2name() != null && !payload.getAct2name().isEmpty()) {
                    Intent btn2 = new Intent(iZooto.appContext, NotificationActionReceiver.class);
//                    btn2.setAction(AppConstant.ACTION_BTN_TWO);
                    btn2.putExtra(AppConstant.KEY_WEB_URL, link2);
                    btn2.putExtra(AppConstant.KEY_NOTIFICITON_ID, notificaitionId);
                    btn2.putExtra(AppConstant.KEY_IN_APP, payload.getInapp());
                    PendingIntent pendingIntent2 = PendingIntent.getBroadcast(iZooto.appContext, new Random().nextInt(100), btn2, PendingIntent.FLAG_UPDATE_CURRENT);
                    NotificationCompat.Action action2 =
                            new NotificationCompat.Action.Builder(
                                    0, payload.getAct2name(), pendingIntent2
                            ).build();
                    notificationBuilder.addAction(action2);
                }
                assert notificationManager != null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    NotificationChannel channel = new NotificationChannel(channelId,
                            "Channel human readable title",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    notificationManager.createNotificationChannel(channel);
                }
                notificationManager.notify(notificaitionId, notificationBuilder.build());
                try {
                    String apiUrl = ApiConstants.NOTIFICAIOT_IMPRESSION_API + PreferenceUtil.getInstance(iZooto.appContext).getStringData(AppConstant.FCM_DEVICE_TOKEN) + "&pid=" + iZooto.mIzooToAppId +
                            "&rid=" + payload.getRid();
                    RestClient.get(apiUrl, new RestClient.ResponseHandler() {

                        @Override
                        void onFailure(int statusCode, String response, Throwable throwable) {
                            super.onFailure(statusCode, response, throwable);
                        }

                        @Override
                        void onSuccess(String response) {
                            super.onSuccess(response);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

                notificationBanner = null;
                notificationIcon = null;
                link = "";
                link1 = "";
                link2 = "";


            }
        };


        new AppExecutors().networkIO().execute(new Runnable() {
            @Override
            public void run() {
                String smallIcon = payload.getIcon();
                String banner = payload.getBanner();
                try {
                    if (smallIcon != null && !smallIcon.isEmpty())
                        notificationIcon = Util.getBitmapFromURL(smallIcon);
                    if (banner != null && !banner.isEmpty()) {
                        notificationBanner = Util.getBitmapFromURL(banner);

                    }
                    handler.post(notificationRunnable);
                } catch (Exception e) {
                    Lg.e("Error", e.getMessage());
                    e.printStackTrace();
                    handler.post(notificationRunnable);
                }
            }
        });
    }


    private static String getFinalUrl(Payload payload) {
        byte[] data = new byte[0];
        try {
            data = payload.getLink().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String encodedLink = Base64.encodeToString(data, Base64.DEFAULT);
        Uri builtUri = Uri.parse(payload.getLink())
                .buildUpon()
                .appendQueryParameter("id", payload.getId())
                .appendQueryParameter("client", payload.getKey())
                .appendQueryParameter("rid", payload.getRid())
                .appendQueryParameter("bkey", PreferenceUtil.getInstance(iZooto.appContext).getStringData(AppConstant.FCM_DEVICE_TOKEN))
                .appendQueryParameter("frwd", encodedLink)
                .build();
        return builtUri.toString();
    }

}
