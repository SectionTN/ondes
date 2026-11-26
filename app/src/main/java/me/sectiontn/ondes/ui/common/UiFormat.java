package me.sectiontn.ondes.ui.common;

import android.content.Context;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.sectiontn.ondes.R;

/** Numbers and dates in the language of the screen, so a French screen never shows English dates. */
public final class UiFormat {

    private UiFormat() {
    }

    /** The language of the strings Android picked: values-fr sets language_code to "fr". */
    public static Locale locale(Context context) {
        return new Locale(context.getString(R.string.language_code));
    }

    /** "1,760" in English, "1 760" in French. */
    public static NumberFormat numbers(Context context) {
        return NumberFormat.getIntegerInstance(locale(context));
    }

    /** Formats with a SimpleDateFormat pattern such as "dd MMM". The dots of French abbreviations ("nov.") are dropped. */
    public static String date(Context context, String pattern, Date date) {
        return new SimpleDateFormat(pattern, locale(context)).format(date).replace(".", "");
    }
}
