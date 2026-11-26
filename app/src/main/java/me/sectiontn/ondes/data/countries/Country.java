package me.sectiontn.ondes.data.countries;

import java.util.List;
import java.util.Locale;

/** One of the 195 countries in assets/countries.json. Gson fills the fields by name. */
public class Country {

    public String code;
    public String name;
    /** The French name, shown when the app runs in French. */
    public String nameFr;
    /** Africa, Americas, Asia, Europe or Oceania. */
    public String continent;
    /** Finer area such as "South America" or "Western Europe". */
    public String subregion;
    /** Main spoken languages, English names. */
    public List<String> languages;
    /** True when Radio Browser has enough stations for this country to appear in a game. */
    public boolean dailyPool;

    /** The Continent hint: the Americas are split into North, Central, South and the Caribbean. */
    public String continentHint() {
        return "Americas".equals(continent) && subregion != null && !subregion.isEmpty() ? subregion : continent;
    }

    /** True when this country speaks the language, ignoring case ("portuguese" matches "Portuguese"). */
    public boolean speaks(String language) {
        if (language == null || languages == null) {
            return false;
        }
        String wanted = language.trim().toLowerCase(Locale.ROOT);
        for (String spoken : languages) {
            if (spoken.toLowerCase(Locale.ROOT).equals(wanted)) {
                return true;
            }
        }
        return false;
    }
}
