package me.sectiontn.ondes.data.api;

import com.google.gson.annotations.SerializedName;

/** A station as Radio Browser returns it. Only the fields the app uses are listed. */
public class StationDto {

    @SerializedName("stationuuid")
    public String uuid;
    public String name;
    public String url;
    @SerializedName("url_resolved")
    public String urlResolved;
    public String favicon;
    /** Comma separated, for example "news,talk,sports". */
    public String tags;
    @SerializedName("countrycode")
    public String countryCode;
    /** A city or a region, often empty. */
    public String state;
    /** Comma separated and lowercase, for example "portuguese". */
    public String language;
    /** 1 when the stream is HLS. */
    public int hls;
}
