package me.sectiontn.ondes.ui.explore;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.sample.SampleData;

/** Rows of the Saved and Recent lists on Explore: country code, name, place and a heart. */
public class StationAdapter extends RecyclerView.Adapter<StationAdapter.Holder> {

    private final List<SampleData.Station> stations;

    public StationAdapter(List<SampleData.Station> stations) {
        this.stations = stations;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new Holder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        SampleData.Station station = stations.get(position);
        holder.code.setText(station.countryCode);
        holder.name.setText(station.name);
        holder.place.setText(station.place);
        bindHeart(holder.heart, station);
        holder.heart.setOnClickListener(v -> {
            station.favorite = !station.favorite;
            bindHeart(holder.heart, station);
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    private static void bindHeart(MaterialButton heart, SampleData.Station station) {
        heart.setIconResource(station.favorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        int color = station.favorite ? R.color.accent : R.color.text_2;
        heart.setIconTint(ContextCompat.getColorStateList(heart.getContext(), color));
        int label = station.favorite ? R.string.remove_from_favorites : R.string.save_to_favorites;
        heart.setContentDescription(heart.getContext().getString(label, station.name));
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
