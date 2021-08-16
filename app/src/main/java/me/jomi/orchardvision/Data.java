package me.jomi.orchardvision;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Data {
    public static class Tree {
        public final int id;
        public final String variant;
        public final double latitude;
        public final double longitude;

        public Tree(int id, String variant, double latitude, double longitude) {
            this.id = id;
            this.variant = variant;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static boolean needDownload = true;

    // type: [variants]
    private static Map<String, List<String>> types = new HashMap<>();
    private static List<Tree> trees = new ArrayList<>();


    public static boolean isNeedDownload() {
        return needDownload;
    }

    private static Thread downloadingThread;
    public static void download(String serverUrl) {
        if (downloadingThread != null)
            downloadingThread.interrupt();
        downloadingThread = new Thread(() -> {
                try {
                    Data._download(serverUrl);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }});
        downloadingThread.start();
    }
    private static void _download(String serverUrl) throws IOException, JSONException {
        String url = serverUrl + "broker/initinfo";

        Map<String, List<String>> types = new HashMap<>();
        List<Tree> trees = new ArrayList<>();


        HttpURLConnection client = (HttpURLConnection) new URL(url).openConnection();
        client.setDoInput(true);

        client.connect();


        JSONObject json;

        try (Scanner in = new Scanner(client.getInputStream())) {
            StringBuilder strB = new StringBuilder();
            while (in.hasNext())
                strB.append(in.next());
            json = new JSONObject(strB.toString());
        }

        JSONObject jsonTypes = json.getJSONObject("types");
        for (Iterator<String> it = jsonTypes.keys(); it.hasNext(); ) {
            String type = it.next();
            List<String> variantList = new ArrayList<>();
            JSONArray array = jsonTypes.getJSONArray(type);
            for (int i=0; i < array.length(); i++)
                variantList.add(array.getString(i));
            types.put(type, variantList);
        }

        JSONObject jsonTrees = json.getJSONObject("types");
        for (Iterator<String> it = jsonTrees.keys(); it.hasNext(); ) {
            String variant = it.next();
            JSONArray array = jsonTrees.getJSONArray(variant);
            for (int i=0; i < array.length(); i++) {
                JSONObject tree = array.getJSONObject(i);
                trees.add(new Tree(tree.getInt("id"), variant, tree.getDouble("latitude"), tree.getDouble("longitude")));
            }
        }


        downloadingThread = null;
        needDownload = false;

        Data.types = types;
        Data.trees = trees;
    }
}
