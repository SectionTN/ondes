package me.sectiontn.ondes.data;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import me.sectiontn.ondes.data.api.StationDto;

/** A radio station as the app uses it, whether it came from the API or from the database. */
public class Station {

    public final String uuid;
    public final String name;
    public final String streamUrl;
    /** Logo URL, often empty. */
    public final String favicon;
    public final List<String> tags;
    public final String countryCode;
    /** A city or region, may be empty. */
    public final String state;
    public final List<String> languages;
    public final boolean hls;

    public Station(String uuid, String name, String streamUrl, String favicon, List<String> tags,
                   String countryCode, String state, List<String> languages, boolean hls) {
        this.uuid = uuid;
        this.name = name.trim();
        this.streamUrl = streamUrl;
        this.favicon = favicon == null ? "" : favicon;
        this.tags = Collections.unmodifiableList(tags);
        this.countryCode = countryCode == null ? "" : countryCode.toUpperCase(Locale.ROOT);
        String place = state == null ? "" : state.trim();
        // Some stations write their region in lowercase, "indiana" shows as "Indiana"
        this.state = place.equals(place.toLowerCase(Locale.ROOT)) ? titleCase(place) : place;
        this.languages = Collections.unmodifiableList(languages);
        this.hls = hls;
    }

    /** Converts an API station, or returns null when it has no name or no stream to play. */
    @Nullable
    public static Station fromDto(StationDto dto) {
        String url = dto.urlResolved != null && !dto.urlResolved.isEmpty() ? dto.urlResolved : dto.url;
        if (dto.uuid == null || dto.name == null || dto.name.trim().isEmpty() || url == null || url.isEmpty()) {
            return null;
        }
        return new Station(dto.uuid, dto.name, url, dto.favicon, splitList(dto.tags), dto.countryCode,
                dto.state, splitList(dto.language), dto.hls == 1);
    }

    /** "news,talk, sports" gives [News, Talk, Sports]. */
    public static List<String> splitList(@Nullable String text) {
        List<String> items = new ArrayList<>();
        if (text == null) {
            return items;
        }
        for (String part : text.split(",")) {
            String item = part.trim();
            if (!item.isEmpty()) {
                items.add(titleCase(item));
            }
        }
        return items;
    }

    /** "rio grande do sul" gives "Rio Grande Do Sul". */
    public static String titleCase(String text) {
        StringBuilder out = new StringBuilder(text.length());
        boolean startOfWord = true;
        for (char c : text.toCharArray()) {
            out.append(startOfWord ? Character.toUpperCase(c) : c);
            startOfWord = c == ' ' || c == '-';
        }
        return out.toString();
    }

    /** The tags joined back with commas, as the database stores them. */
    public String tagsText() {
        return joinList(tags);
    }

    public String languagesText() {
        return joinList(languages);
    }

    // String.join needs Android 8, the app starts at Android 7
    private static String joinList(List<String> items) {
        StringBuilder text = new StringBuilder();
        for (String item : items) {
            if (text.length() > 0) {
                text.append(',');
            }
            text.append(item);
        }
        return text.toString();
    }
}
