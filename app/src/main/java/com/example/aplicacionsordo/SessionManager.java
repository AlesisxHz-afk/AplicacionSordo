package com.example.aplicacionsordo;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "AppUserSession";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NOMBRES = "nombres";
    private static final String KEY_APE_PATERNO = "ape_paterno";
    private static final String KEY_APE_MATERNO = "ape_materno";
    private static final String KEY_PHOTO_URI = "photo_uri";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(User user) {
        if (user == null) return;
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, user.getUsuarioId());
        editor.putString(KEY_USERNAME, user.getUsuario());
        editor.putString(KEY_NOMBRES, user.getNombres());
        editor.putString(KEY_APE_PATERNO, user.getApePaterno());
        editor.putString(KEY_APE_MATERNO, user.getApeMaterno());
        editor.putString(KEY_PHOTO_URI, user.getProfilePhotoUri());
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public User getUserSession() {
        if (!isLoggedIn()) return null;
        int id = prefs.getInt(KEY_USER_ID, 1);
        String username = prefs.getString(KEY_USERNAME, "Usuario");
        String nombres = prefs.getString(KEY_NOMBRES, "");
        String apePaterno = prefs.getString(KEY_APE_PATERNO, "");
        String apeMaterno = prefs.getString(KEY_APE_MATERNO, "");
        String photoUri = prefs.getString(KEY_PHOTO_URI, null);

        return new User(id, username, nombres, apePaterno, apeMaterno, true, photoUri);
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
