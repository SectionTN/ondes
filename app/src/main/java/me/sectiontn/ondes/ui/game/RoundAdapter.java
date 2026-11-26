package me.sectiontn.ondes.ui.game;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.data.countries.CountryCatalog;
import me.sectiontn.ondes.game.Outcome;
import me.sectiontn.ondes.game.RoundRecord;

/** The five rounds on the Summary card. */
public class RoundAdapter extends RecyclerView.Adapter<RoundAdapter.Holder> {

    private final List<RoundRecord> rounds;
    private final CountryCatalog catalog;

    public RoundAdapter(List<RoundRecord> rounds, CountryCatalog catalog) {
        this.rounds = rounds;
        this.catalog = catalog;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_round, parent, false);
        return new Holder(row);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        RoundRecord round = rounds.get(position);
        Context context = holder.itemView.getContext();

        ViewCompat.setBackgroundTintList(holder.square,
                ContextCompat.getColorStateList(context, OutcomeStyle.color(round.outcome)));
        holder.square.setImageResource(OutcomeStyle.icon(round.outcome));
        holder.square.setContentDescription(context.getString(OutcomeStyle.description(round.outcome)));

        holder.country.setText(catalog.nameOf(round.countryCode));
        boolean wrong = round.outcome == Outcome.WRONG;
        if (wrong) {
            holder.detail.setText(round.guessCode != null
                    ? context.getString(R.string.you_said, catalog.nameOf(round.guessCode))
                    : context.getString(R.string.times_up));
        } else if (round.state == null || round.state.isEmpty()) {
            holder.detail.setText(round.stationName);
        } else {
            holder.detail.setText(context.getString(R.string.round_detail, round.stationName, round.state));
        }
        holder.points.setText(wrong ? context.getString(R.string.points_none) : context.getString(R.string.points_plain, round.points));
        holder.points.setTextColor(ContextCompat.getColor(context, wrong ? R.color.wrong_text : R.color.text));
    }

    @Override
    public int getItemCount() {
        return rounds.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView square;
        final TextView country;
        final TextView detail;
        final TextView points;

        Holder(View row) {
            super(row);
            square = row.findViewById(R.id.round_square);
            country = row.findViewById(R.id.round_country);
            detail = row.findViewById(R.id.round_detail);
            points = row.findViewById(R.id.round_points);
        }
    }
}
