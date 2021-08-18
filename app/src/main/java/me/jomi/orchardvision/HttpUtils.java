package me.jomi.orchardvision;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Pair;
import me.jomi.orchardvision.interfaces.Consumer;
import me.jomi.orchardvision.interfaces.ConsumerUnSafe;
import me.jomi.orchardvision.interfaces.Supplier;
import me.jomi.orchardvision.json.Json;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;

public abstract  class HttpUtils {
    private static class URLs {
        static final String serverHost = "http://6.tcp.eu.ngrok.io:16788/";

        static final String init = serverHost + "broker/initinfo";

        static final String treeNew = serverHost + "broker/new/tree";
        static final String treeEdit = serverHost + "broker/edit/tree";
        static final String treeInfo = serverHost + "broker/info/tree/"; // needs an id at the end
        static final String treeDelete = serverHost + "broker/delete/tree/"; // needs an id at the end

        static final String treeMarker = serverHost + "static/orchardMap/img/treeMarker.png";

    }
     private static class Requests {
        /**
         * Send GET request
         *
         * @param url to send request
         * @return inputStream of response
         */
        static InputStream sendRequest(String url) throws IOException {
            HttpURLConnection client = (HttpURLConnection) new URL(url).openConnection();
            client.setDoInput(true);

            client.connect();

            return client.getInputStream();
        }

        /**
         * Send POST request
         *
         * @param url to send request
         * @param postData data in post request
         * @return inputStream of response
         */
        static InputStream sendRequest(String url, Pair<String, String>... postData) throws IOException {
            byte[] dataBytes = getPostBytes(postData);

            HttpURLConnection client = (HttpURLConnection) new URL(url).openConnection();

            client.setDoOutput(true);
            client.setUseCaches(false);
            client.setInstanceFollowRedirects(false);

            client.setRequestMethod("POST");
            client.setRequestProperty("charset", "utf-8");
            client.setRequestProperty("Content-Length", Integer.toString(dataBytes.length));

            try(DataOutputStream wr = new DataOutputStream(client.getOutputStream())) {
                wr.write(dataBytes);
                wr.flush();
            }

            return client.getInputStream();
        }
        private static byte[] getPostBytes(Pair<String, String>... data) throws UnsupportedEncodingException {
             StringBuilder strB = new StringBuilder();

             for (Pair<String, String> pair : data) {
                 if (strB.length() > 0)
                     strB.append('&');
                 strB.append(URLEncoder.encode(pair.first, "utf-8"))
                         .append('=')
                         .append(URLEncoder.encode(pair.second, "utf-8"));
             }

             return strB.toString().getBytes("utf-8");
         }

        static Json init() throws IOException {
            InputStream stream = sendRequest(URLs.init);
            String data = Func.readData(stream);
            return new Json(data);
        }

        static Bitmap downloadMarkerIcon(int scale) throws IOException {
            byte[] data;
            try (InputStream stream = sendRequest(URLs.treeMarker)) {
                data = Func.toByteArray(stream);
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            return Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * scale, bitmap.getHeight() * scale, false);
        }

        static Json downloadTree(int id) throws IOException {
            InputStream stream = sendRequest(URLs.treeInfo + id);
            String response = Func.readData(stream);
            return new Json(response);
        }

        static String sendTree(Pair<String, String>... data) throws IOException {
            InputStream stream = sendRequest(URLs.treeNew, data);
            String response = Func.readData(stream);
            return response;
        }

        static Void editTree(Pair<String, String>... data) throws IOException {
            sendRequest(URLs.treeEdit, data).close();
            return null;
        }

        static Void deleteTree(int id) throws IOException {
            sendRequest(URLs.treeDelete + id).close();
            return null;
        }
    }

    private static final Handler mHandler = new Handler();
    private static <T> void post(Consumer<T> callback, T value) {
        mHandler.post(() -> callback.accept(value));
    }

    private static Set<String> problems = new HashSet<>();
    private static void problem(String msg) {
        if (problems.add(msg))
            mHandler.post(() -> new AlertDialog.Builder(OrchardVision.curActivity)
                    .setMessage(msg)
                    .setNeutralButton("ok", ((dialog, which) -> problems.remove(msg)))
                    .show()
        );
    }
    private static void problemIO(String msg) {
        problem("Problem z internetem\n" + msg);
    }

    /**
     * Sends request to server, callback/fallback to app and if fallback returns true trying again send request
     *
     * @param callback callback to call when sends request successful
     * @param request request to send
     * @param problemIO problem to display for user when request fail
     * @param fallback when request fail
     * @param <T>
     */
    private static <T> void factor(Consumer<T> callback, Callable<T> request, String problemIO, Supplier<Boolean> fallback) {
        new Thread(() -> {
            T res;

            try {
                res = request.call();
            } catch (Exception e) {
                problemIO(problemIO);
                if (fallback.get())
                    mHandler.postDelayed(() -> factor(callback, request, problemIO, fallback), 10_000L);
                return;
            }

            if (callback != null)
                post(callback, res);
        }).start();
    }
    private static <T> void factor(Consumer<T> callback, Callable<T> require, String problemIO, boolean tryAgain) {
        factor(callback, require, problemIO, () -> tryAgain);
    }



    // public

    public static void init(Consumer<Json> callback) {
        factor(callback, Requests::init, "", true);
    }
    public static void downloadMarkerIcon(Consumer<Bitmap> callback) {
        factor(callback, () -> Requests.downloadMarkerIcon(3), "", true);
    }
    public static void downloadTree(int id) {
        factor(json -> {
            Data.Tree tree = Data.Tree.fromId.containsKey(id) ? Data.Tree.fromId.get(id) : new Data.Tree(json);

            tree.needDownload = false;
            mHandler.postDelayed(() -> tree.needDownload = true, 30_000L);

            Data.addTypeVariant(tree.getType(), tree.getVariant());

            tree.update(json);
        }, () -> Requests.downloadTree(id), "Nie udało pobrać się informacji o drzewie", true);
    }
    public static void editTree(Consumer<Void> callback, Pair<String, String>... data) {
        factor(callback, () -> Requests.editTree(data), "", true);
    }
    public static void deleteTree(int id) {
        factor(__ -> Func.doForNonNull(Data.Tree.fromId.get(id), Data.Tree::destroy), () -> Requests.deleteTree(id), "Nie udało się usunąć drzewa", false);
    }
    public static void sendTree(Consumer<Integer> callback, Pair<String, String>... data) {
        factor(callback, () -> {
            try {
                return Integer.parseInt(Requests.sendTree(data));
            } catch (NumberFormatException e) {
                problem("Problem ze zgodnością danych");
                return -1;
            }
        }, "", true);
    }
}
