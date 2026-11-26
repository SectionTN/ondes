package me.sectiontn.ondes.data.countries;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.sectiontn.ondes.R;

/** The 195 countries of the game, read once from assets/countries.json. */
public final class CountryCatalog {

    public static final String[] CONTINENTS = {"Africa", "Americas", "Asia", "Europe", "Oceania"};

    private static CountryCatalog instance;

    private final List<Country> all;
    private final List<Country> dailyPool = new ArrayList<>();
    private final Map<String, Country> byCode = new HashMap<>();
    // Null in the unit tests, where every name stays in English
    @Nullable
    private final Context appContext;
    // English language names to ISO codes, such as "portuguese" to "pt", filled on first use
    @Nullable
    private Map<String, String> languageCodes;

    /** Builds a catalog from a list, used by the unit tests. */
    public CountryCatalog(List<Country> countries) {
        this(countries, null);
    }

    private CountryCatalog(List<Country> countries, @Nullable Context appContext) {
        this.appContext = appContext;
        all = Collections.unmodifiableList(new ArrayList<>(countries));
        for (Country country : all) {
            byCode.put(country.code, country);
            if (country.dailyPool) {
                dailyPool.add(country);
            }
        }
    }

    public static synchronized CountryCatalog get(Context context) {
        if (instance == null) {
            Context app = context.getApplicationContext();
            try (Reader reader = new InputStreamReader(app.getAssets().open("countries.json"), StandardCharsets.UTF_8)) {
                List<Country> countries = new Gson().fromJson(reader, new TypeToken<List<Country>>() { }.getType());
                instance = new CountryCatalog(countries, app);
            } catch (IOException e) {
                // The file ships inside the APK, so a failure here is a packaging bug
                throw new IllegalStateException("countries.json is missing from the APK", e);
            }
        }
        return instance;
    }

    public List<Country> all() {
        return all;
    }

    /** Countries with enough stations to be played, sorted by code so the daily pick is stable. */
    public List<Country> dailyPool() {
        return Collections.unmodifiableList(dailyPool);
    }

    @Nullable
    public Country find(String code) {
        return code == null ? null : byCode.get(code.toUpperCase(Locale.ROOT));
    }

    /** The country's name in the language of the screen. */
    public String nameOf(Country country) {
        return isFrench() && country.nameFr != null ? country.nameFr : country.name;
    }

    /** The country's name, or the code itself when it is not one of the 195. */
    public String nameOf(String code) {
        Country country = find(code);
        return country != null ? nameOf(country) : code;
    }

    /** A continent or an Americas subregion, such as "South America", in the language of the screen. */
    public String continentName(String continent) {
        if (appContext == null) {
            return continent;
        }
        switch (continent) {
            case "Africa": return appContext.getString(R.string.continent_africa);
            case "Americas": return appContext.getString(R.string.continent_americas);
            case "Asia": return appContext.getString(R.string.continent_asia);
            case "Europe": return appContext.getString(R.string.continent_europe);
            case "Oceania": return appContext.getString(R.string.continent_oceania);
            case "North America": return appContext.getString(R.string.continent_north_america);
            case "Central America": return appContext.getString(R.string.continent_central_america);
            case "South America": return appContext.getString(R.string.continent_south_america);
            case "Caribbean": return appContext.getString(R.string.continent_caribbean);
            default: return continent;
        }
    }

    /** A Radio Browser language such as "Portuguese", in French when the screen is French. */
    public String languageName(String english) {
        if (!isFrench()) {
            return english;
        }
        if (languageCodes == null) {
            languageCodes = new HashMap<>();
            for (String code : Locale.getISOLanguages()) {
                languageCodes.put(new Locale(code).getDisplayLanguage(Locale.ENGLISH).toLowerCase(Locale.ROOT), code);
            }
            // Names Radio Browser uses that differ from Java's English names
            languageCodes.put("bengali", "bn");
            languageCodes.put("slovene", "sl");
            languageCodes.put("farsi", "fa");
            languageCodes.put("mandarin", "zh");
            languageCodes.put("filipino", "fil");
            languageCodes.put("brazilian portuguese", "pt");
        }
        String code = languageCodes.get(english.toLowerCase(Locale.ROOT));
        if (code == null) {
            return english;
        }
        // Java writes French language names in lowercase ("portugais"), the hint tile starts with a capital
        String french = new Locale(code).getDisplayLanguage(Locale.FRENCH);
        return french.substring(0, 1).toUpperCase(Locale.FRENCH) + french.substring(1);
    }

    public int size() {
        return all.size();
    }

    // values-fr/strings.xml sets language_code to "fr", so this follows the strings Android picked
    private boolean isFrench() {
        return appContext != null && "fr".equals(appContext.getString(R.string.language_code));
    }
}
