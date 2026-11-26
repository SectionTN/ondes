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

/** The 195 countries of the game, read once from assets/countries.json. */
public final class CountryCatalog {

    public static final String[] CONTINENTS = {"Africa", "Americas", "Asia", "Europe", "Oceania"};

    private static CountryCatalog instance;

    private final List<Country> all;
    private final List<Country> dailyPool = new ArrayList<>();
    private final Map<String, Country> byCode = new HashMap<>();

    /** Builds a catalog from a list, used by the unit tests. */
    public CountryCatalog(List<Country> countries) {
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
            try (Reader reader = new InputStreamReader(
                    context.getApplicationContext().getAssets().open("countries.json"), StandardCharsets.UTF_8)) {
                List<Country> countries = new Gson().fromJson(reader, new TypeToken<List<Country>>() { }.getType());
                instance = new CountryCatalog(countries);
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

    /** The country's name, or the code itself when it is not one of the 195. */
    public String nameOf(String code) {
        Country country = find(code);
        return country != null ? country.name : code;
    }

    public int size() {
        return all.size();
    }
}
