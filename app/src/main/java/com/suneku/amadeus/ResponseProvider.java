package com.suneku.amadeus;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

class Response {
    private int mood;
    private String subtitle;

    int getMood() {
        return mood;
    }

    String getSubtitle() {
        return subtitle;
    }

    Response(int mood, String subtitle) {
        this.mood = mood;
        this.subtitle = subtitle;
    }
}

class ResponseProvider {

    private HashMap<String, ArrayList<Response>> map = new HashMap<>();
    private ArrayList<Response> responsesNotFound = new ArrayList<>();

    private int getResId(String resName) {
        try {
            Class res = R.drawable.class;
            Field field = res.getField(resName);
            return field.getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    ResponseProvider(Context context, String appLang, String spriteVariation) {
        try {
            // Responses map
            HashMap<String, Response> responses = new HashMap<>();

            // Responses
            InputStream stream = context.getAssets().open(appLang + "/responses.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(";");
                int mood = getResId("kurisu_" + parts[1].toLowerCase() + "_" + spriteVariation);
                responses.put(parts[0].toLowerCase(), new Response(mood, parts[2]));
            }
            // Not found responses
            stream = context.getAssets().open(appLang + "/responses-not_found.csv");
            reader = new BufferedReader(new InputStreamReader(stream));
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(";");
                int mood = getResId("kurisu_" + parts[0].toLowerCase() + "_" + spriteVariation);
                responsesNotFound.add(new Response(mood, parts[1]));
            }
            // Triggers
            /*
             * FIXME: Triggers in code
             * Probably the only way to use all responses around the code
             * is putting them in a single triggers.csv file.
             * Might be a problem when file becomes big.
             */
            stream = context.getAssets().open("triggers.csv");
            reader = new BufferedReader(new InputStreamReader(stream));
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(";");
                ArrayList<Response> variants = new ArrayList<>();
                for (String rsp : parts[1].split(",")) {
                    variants.add(responses.get(rsp));
                }
                map.put(parts[0].toLowerCase(), variants);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Response getResponse(String input) {

        Random random = new Random();

        boolean found = false;
        ArrayList<Response> variants = new ArrayList<>();

        for (String key : map.keySet()) {
            if (input.contains(key)) {
                variants = map.get(key);
                found = true;
                break;
            }
        }

        if (!found) {
            variants = responsesNotFound;
        }

        return variants.get(random.nextInt(variants.size()));
    }

}
