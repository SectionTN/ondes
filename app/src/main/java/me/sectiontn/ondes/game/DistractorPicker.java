package me.sectiontn.ondes.game;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import me.sectiontn.ondes.data.countries.Country;

/**
 * Picks the three wrong answers. Countries that speak the station's language come first,
 * then countries of the same continent, then any country, so the choice is never obvious.
 */
public final class DistractorPicker {

    public static final int OPTION_COUNT = 4;

    private DistractorPicker() {
    }

    /**
     * @param stationLanguage the station's first language, or null to use the country's own
     * @param countries       every country that can be a wrong answer, in a fixed order
     * @param random          seeded for the daily, so every player gets the same options
     * @return four countries, the answer among them, in shuffled order
     */
    public static List<Country> options(Country answer, @Nullable String stationLanguage,
                                        List<Country> countries, Random random) {
        String language = stationLanguage;
        if ((language == null || language.isEmpty()) && answer.languages != null && !answer.languages.isEmpty()) {
            language = answer.languages.get(0);
        }

        List<Country> sameLanguage = new ArrayList<>();
        List<Country> sameContinent = new ArrayList<>();
        List<Country> others = new ArrayList<>();
        for (Country country : countries) {
            if (country.code.equals(answer.code)) {
                continue;
            }
            if (country.speaks(language)) {
                sameLanguage.add(country);
            } else if (country.continent.equals(answer.continent)) {
                sameContinent.add(country);
            } else {
                others.add(country);
            }
        }
        Collections.shuffle(sameLanguage, random);
        Collections.shuffle(sameContinent, random);
        Collections.shuffle(others, random);

        List<Country> options = new ArrayList<>();
        options.add(answer);
        for (List<Country> group : Arrays.asList(sameLanguage, sameContinent, others)) {
            for (Country country : group) {
                if (options.size() == OPTION_COUNT) {
                    break;
                }
                options.add(country);
            }
        }
        Collections.shuffle(options, random);
        return options;
    }
}
