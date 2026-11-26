package me.sectiontn.ondes.data.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** The Radio Browser endpoints the app calls. Retrofit writes the implementation. */
public interface RadioBrowserApi {

    /** Working stations of one country, for example countrycode=BR, order=votes, reverse=true. */
    @GET("json/stations/search?hidebroken=true")
    Call<List<StationDto>> byCountry(@Query("countrycode") String countryCode,
                                     @Query("order") String order,
                                     @Query("reverse") boolean reverse,
                                     @Query("limit") int limit);

    /** Working stations with one exact tag. A null tag is left out of the URL, which means every genre. */
    @GET("json/stations/search?hidebroken=true&tagExact=true")
    Call<List<StationDto>> byTag(@Query("tag") String tag,
                                 @Query("order") String order,
                                 @Query("limit") int limit);

    /** Counts one play of a station. Radio Browser asks apps to call it when the user picks a station. */
    @GET("json/url/{uuid}")
    Call<Void> click(@Path("uuid") String uuid);
}
