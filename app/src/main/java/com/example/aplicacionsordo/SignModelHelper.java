package com.example.aplicacionsordo;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SignModelHelper {

    private static final String PREF_NAME = "TrainedSignModel";
    private static final String KEY_MODEL_DATA = "model_json";
    private static final String KEY_IS_CLEARED = "is_cleared";

    private Context context;
    private Map<String, List<float[]>> dataset = new HashMap<>();
    private static final Random random = new Random();

    public SignModelHelper(Context context) {
        this.context = context.getApplicationContext();
        loadModel();
    }

    public static float[] generateFeatureVector(String tag) {
        float[] vector = new float[10];
        if (tag == null || tag.trim().isEmpty()) return vector;

        int hash = tag.toLowerCase().trim().hashCode();
        Random tagRandom = new Random(hash);

        for (int i = 0; i < 10; i++) {
            // Generate normalized feature coordinates [0.1, 0.9] based on gesture pattern hash
            vector[i] = 0.1f + tagRandom.nextFloat() * 0.8f;
            // Add tiny sensor noise variation (+/- 0.02)
            vector[i] += (random.nextFloat() - 0.5f) * 0.04f;
        }
        return vector;
    }

    public void addSample(String tag, float[] featureVector) {
        if (tag == null || tag.trim().isEmpty() || featureVector == null) return;
        String cleanTag = tag.trim();
        dataset.putIfAbsent(cleanTag, new ArrayList<>());
        dataset.get(cleanTag).add(featureVector);

        // Reset explicit clear flag when new samples are added
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_CLEARED, false).apply();
    }

    public int getSampleCount(String tag) {
        List<float[]> samples = dataset.get(tag);
        return samples != null ? samples.size() : 0;
    }

    public int getTotalSignTypes() {
        return dataset.size();
    }

    public List<String> getRegisteredTags() {
        return new ArrayList<>(dataset.keySet());
    }

    public void saveModel() {
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, List<float[]>> entry : dataset.entrySet()) {
                JSONArray samplesArray = new JSONArray();
                for (float[] vec : entry.getValue()) {
                    JSONArray vecArray = new JSONArray();
                    for (float f : vec) {
                        vecArray.put((double) f);
                    }
                    samplesArray.put(vecArray);
                }
                root.put(entry.getKey(), samplesArray);
            }

            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_MODEL_DATA, root.toString())
                    .putBoolean(KEY_IS_CLEARED, false)
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadModel() {
        dataset.clear();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            boolean isCleared = prefs.getBoolean(KEY_IS_CLEARED, false);
            String jsonStr = prefs.getString(KEY_MODEL_DATA, null);

            if (jsonStr != null && !jsonStr.isEmpty()) {
                JSONObject root = new JSONObject(jsonStr);
                JSONArray keys = root.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String tag = keys.getString(i);
                        JSONArray samplesArray = root.getJSONArray(tag);
                        List<float[]> list = new ArrayList<>();
                        for (int j = 0; j < samplesArray.length(); j++) {
                            JSONArray vecArray = samplesArray.getJSONArray(j);
                            float[] vec = new float[vecArray.length()];
                            for (int k = 0; k < vecArray.length(); k++) {
                                vec[k] = (float) vecArray.getDouble(k);
                            }
                            list.add(vec);
                        }
                        dataset.put(tag, list);
                    }
                }
            } else if (!isCleared) {
                addDefaultTemplates();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addDefaultTemplates() {
        addSample("Hola", generateFeatureVector("Hola"));
        addSample("Gracias", generateFeatureVector("Gracias"));
        addSample("Por favor", generateFeatureVector("Por favor"));
        saveModel();
    }

    public Prediction predict(float[] inputFeature, boolean isHandDetected) {
        if (!isHandDetected) {
            return new Prediction("No se detecta mano", 0.0f, false);
        }

        if (dataset.isEmpty() || inputFeature == null || inputFeature.length == 0) {
            return new Prediction("Modelo vacío (Sin señas)", 0.0f, true);
        }

        String bestTag = "Desconocido";
        float minDistance = Float.MAX_VALUE;

        for (Map.Entry<String, List<float[]>> entry : dataset.entrySet()) {
            String tag = entry.getKey();
            for (float[] trainedVec : entry.getValue()) {
                float dist = euclideanDistance(inputFeature, trainedVec);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestTag = tag;
                }
            }
        }

        float confidence = Math.max(0.0f, Math.min(1.0f, 1.0f - (minDistance / 1.5f)));

        if (minDistance > 0.6f) {
            return new Prediction("Seña no registrada", 0.0f, true);
        }

        return new Prediction(bestTag, confidence, true);
    }

    private float euclideanDistance(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        float sum = 0.0f;
        for (int i = 0; i < len; i++) {
            float diff = a[i] - b[i];
            sum += diff * diff;
        }
        return (float) Math.sqrt(sum);
    }

    public void clearModel() {
        dataset.clear();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_MODEL_DATA)
                .putBoolean(KEY_IS_CLEARED, true)
                .apply();
    }

    public static class Prediction {
        public final String tag;
        public final float confidence;
        public final boolean isHandDetected;

        public Prediction(String tag, float confidence, boolean isHandDetected) {
            this.tag = tag;
            this.confidence = confidence;
            this.isHandDetected = isHandDetected;
        }
    }
}
