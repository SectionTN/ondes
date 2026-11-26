package me.sectiontn.ondes.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import me.sectiontn.ondes.data.countries.Country;

public class DistractorPickerTest {

    private static Country country(String code, String continent, String... languages) {
        Country country = new Country();
        country.code = code;
        country.name = code;
        country.continent = continent;
        country.subregion = "";
        country.languages = Arrays.asList(languages);
        country.dailyPool = true;
        return country;
    }

    private static final Country BRAZIL = country("BR", "Americas", "Portuguese");
    private static final List<Country> COUNTRIES = Arrays.asList(
            country("AO", "Africa", "Portuguese"),
            country("AR", "Americas", "Spanish"),
            BRAZIL,
            country("CL", "Americas", "Spanish"),
            country("DE", "Europe", "German"),
            country("JP", "Asia", "Japanese"),
            country("MZ", "Africa", "Portuguese"),
            country("PT", "Europe", "Portuguese"));

    private static List<String> codes(List<Country> countries) {
        List<String> codes = new ArrayList<>();
        for (Country c : countries) {
            codes.add(c.code);
        }
        return codes;
    }

    @Test
    public void fourDistinctOptionsWithTheAnswer() {
        List<String> options = codes(DistractorPicker.options(BRAZIL, "portuguese", COUNTRIES, new Random(1)));
        assertEquals(4, options.size());
        assertEquals(4, new HashSet<>(options).size());
        assertTrue(options.contains("BR"));
    }

    @Test
    public void sameLanguageComesFirst_ignoringCase() {
        List<String> options = codes(DistractorPicker.options(BRAZIL, "portuguese", COUNTRIES, new Random(7)));
        assertEquals(new HashSet<>(Arrays.asList("BR", "PT", "AO", "MZ")), new HashSet<>(options));
    }

    @Test
    public void thenTheSameContinent() {
        Country japan = COUNTRIES.get(5);
        // Nobody else speaks Japanese and no other country is in Asia, so the rest come from anywhere
        assertEquals(4, DistractorPicker.options(japan, "japanese", COUNTRIES, new Random(3)).size());
        Country argentina = COUNTRIES.get(1);
        List<String> options = codes(DistractorPicker.options(argentina, "spanish", COUNTRIES, new Random(3)));
        assertTrue(options.contains("CL"));
        assertTrue(options.contains("BR"));
    }

    @Test
    public void sameSeedGivesTheSameOptions() {
        assertEquals(codes(DistractorPicker.options(BRAZIL, null, COUNTRIES, new Random(42))),
                codes(DistractorPicker.options(BRAZIL, null, COUNTRIES, new Random(42))));
    }
}
