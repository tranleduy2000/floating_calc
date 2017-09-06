package com.duy.calculator;

import android.content.Context;
import android.preference.PreferenceManager;

public class CalculatorSettings {
    public static final String PREF_KEY_COLOR_ACCENT = "accent_color";
    public static final String PREF_KEY_OPACITY = "FLOATING_OPACITY";

    public static void setRadiansEnabled(Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("USE_RADIANS", enabled).apply();
    }

    public static boolean useRadians(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("USE_RADIANS", true);
    }

    public static boolean showWidgetBackground(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("SHOW_WIDGET_BACKGROUND", false);
    }

    public static void setOpacity(Context context, int opacity) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(PREF_KEY_OPACITY, opacity).apply();
    }

    public static int getOpacity(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_KEY_OPACITY, 100);
    }
}
