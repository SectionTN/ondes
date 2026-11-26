package me.sectiontn.ondes.ui.explore;

import android.annotation.SuppressLint;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.data.Station;
import me.sectiontn.ondes.ui.common.Motion;

/** Rows of the Saved and Recent lists on Explore: country code, name, place and a heart. */
public class StationAdapter extends RecyclerView.Adapter<StationAdapter.Holder> {

    /** What a row does when tapped. */
    public interface Listener {
        void onPlay(Station station);

        void onFavorite(Station station, boolean favorite);

        String placeOf(Station station);
    }

    private static final long BUMP_WINDOW_MS = 500;

    private final Listener listener;
    private List<Station> stations = new ArrayList<>();
    private Set<String> favoriteIds = new HashSet<>();
    // The station whose heart was just tapped to save it, and until when its filled heart bumps
    @Nullable
    private String bumpId;
    private long bumpUntil;

    public StationAdapter(Listener listener) {
        this.listener = listener;
    }

    // Recent and Saved are short lists, so rebinding every row costs nothing
    @SuppressLint("NotifyDataSetChanged")
    public void setStations(List<Station> stations) {
        this.stations = stations;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setFavoriteIds(Set<String> favoriteIds) {
        this.favoriteIds = favoriteIds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new Holder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Station station = stations.get(position);
        boolean favorite = favoriteIds.contains(station.uuid);
        holder.code.setText(station.countryCode);
        holder.name.setText(station.name);
        holder.place.setText(listener.placeOf(station));
        holder.itemView.setOnClickListener(v -> listener.onPlay(station));
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(R.string.play_station, station.name));

        MaterialButton heart = holder.heart;
        heart.setIconResource(favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        heart.setIconTint(ContextCompat.getColorStateList(heart.getContext(), favorite ? R.color.accent : R.color.text_2));
        int label = favorite ? R.string.remove_from_favorites : R.string.save_to_favorites;
        heart.setContentDescription(heart.getContext().getString(label, station.name));
        heart.setOnClickListener(v -> {
            if (!favorite) {
                // The heart fills a moment later, when the database has saved it: that redraw is the one to bump
                bumpId = station.uuid;
                bumpUntil = SystemClock.uptimeMillis() + BUMP_WINDOW_MS;
            }
            listener.onFavorite(station, !favorite);
        });
        // Rows are redrawn in any order, so a bump left running from another station is stopped first
        heart.animate().cancel();
        heart.setScaleX(1f);
        heart.setScaleY(1f);
        if (favorite && station.uuid.equals(bumpId) && SystemClock.uptimeMillis() < bumpUntil) {
            Motion.pulse(heart);
        }
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView code;
        final TextView name;
        final TextView place;
        final MaterialButton heart;

        Holder(View row) {
            super(row);
            code = row.findViewById(R.id.station_code);
            name = row.findViewById(R.id.station_name);
            place = row.findViewById(R.id.station_place);
            heart = row.findViewById(R.id.station_heart);
        }
    }
}
