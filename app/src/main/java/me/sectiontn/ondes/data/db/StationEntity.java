package me.sectiontn.ondes.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import me.sectiontn.ondes.data.Station;

/** A station the user played in Explore or saved as a favorite. */
@Entity(tableName = "stations")
public class StationEntity {

    @PrimaryKey
    @NonNull
    public String uuid = "";
    public String name;
    public String countryCode;
    public String state;
    public String url;
    public String favicon;
    public String tags;
    public String languages;
    public boolean hls;
    public boolean isFavorite;
    /** When Explore last played it, 0 for a station only saved from a game. */
    public long lastPlayedAt;

    public static StationEntity from(Station station) {
        StationEntity entity = new StationEntity();
        entity.uuid = station.uuid;
        entity.name = station.name;
        entity.countryCode = station.countryCode;
        entity.state = station.state;
        entity.url = station.streamUrl;
        entity.favicon = station.favicon;
        entity.tags = station.tagsText();
        entity.languages = station.languagesText();
        entity.hls = station.hls;
        return entity;
    }

    public Station toStation() {
        return new Station(uuid, name, url, favicon, Station.splitList(tags), countryCode, state,
                Station.splitList(languages), hls);
    }
}
