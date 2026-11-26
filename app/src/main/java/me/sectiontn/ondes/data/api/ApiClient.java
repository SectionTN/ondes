package me.sectiontn.ondes.data.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Talks to Radio Browser, which has no single host: it asks for the server list, shuffles it,
 * and when a server fails it tries the next one.
 */
public final class ApiClient {

    /** Radio Browser asks every app to send a User-Agent that names it. */
    public static final String USER_AGENT = "Ondes/1.0 (Android)";

    private static final String SERVER_LIST_URL = "https://all.api.radio-browser.info/json/servers";
    // Used when the server list itself cannot be fetched
    private static final String[] FALLBACK_SERVERS = {"de1.api.radio-browser.info", "de2.api.radio-browser.info"};
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int READ_TIMEOUT_SECONDS = 10;

    private static ApiClient instance;

    private final OkHttpClient http;
    private final Map<String, RadioBrowserApi> apis = new HashMap<>();
    private List<String> servers;
    private int current = 0;

    /** One Retrofit call to make against whichever server answers. */
    public interface ApiCall<T> {
        Call<T> on(RadioBrowserApi api);
    }

    private ApiClient() {
        http = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder().header("User-Agent", USER_AGENT).build()))
                .build();
    }

    public static synchronized ApiClient get() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    /** Runs the call on the current server, then on each other server until one answers. Blocks, so call it off the main thread. */
    public <T> T execute(ApiCall<T> call) throws IOException {
        List<String> hosts = servers();
        IOException lastError = new IOException("No Radio Browser server");
        for (int attempt = 0; attempt < hosts.size(); attempt++) {
            String host = hosts.get(currentIndex(hosts.size()));
            try {
                Response<T> response = call.on(api(host)).execute();
                if (response.isSuccessful()) {
                    return response.body();
                }
                lastError = new IOException("HTTP " + response.code() + " from " + host);
            } catch (IOException e) {
                lastError = e;
            }
            moveToNextServer(hosts.size());
        }
        throw lastError;
    }

    private synchronized int currentIndex(int size) {
        return current % size;
    }

    private synchronized void moveToNextServer(int size) {
        current = (current + 1) % size;
    }

    private synchronized RadioBrowserApi api(String host) {
        RadioBrowserApi api = apis.get(host);
        if (api == null) {
            api = new Retrofit.Builder()
                    .baseUrl("https://" + host + "/")
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(RadioBrowserApi.class);
            apis.put(host, api);
        }
        return api;
    }

    /** The shuffled server list, fetched once. The fallback hosts are always in it. */
    private synchronized List<String> servers() {
        if (servers != null) {
            return servers;
        }
        List<String> hosts = new ArrayList<>();
        boolean fetched = false;
        try (okhttp3.Response response = http.newCall(new Request.Builder().url(SERVER_LIST_URL).build()).execute()) {
            ResponseBody body = response.body();
            if (response.isSuccessful() && body != null) {
                JsonArray list = new Gson().fromJson(body.string(), JsonArray.class);
                for (JsonElement server : list) {
                    // The list repeats a host once per IP address (IPv4 and IPv6)
                    String name = server.getAsJsonObject().get("name").getAsString();
                    if (!hosts.contains(name)) {
                        hosts.add(name);
                    }
                }
                fetched = true;
            }
        } catch (IOException | RuntimeException e) {
            // No list: the fallback hosts below are enough
        }
        Collections.shuffle(hosts);
        for (String fallback : FALLBACK_SERVERS) {
            if (!hosts.contains(fallback)) {
                hosts.add(fallback);
            }
        }
        // Only a fetched list is kept, so an offline start asks again next time
        if (fetched) {
            servers = hosts;
        }
        return hosts;
    }
}
