package me.sectiontn.ondes.ui.passport;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.data.countries.CountryCatalog;
import me.sectiontn.ondes.data.db.StampEntity;
import me.sectiontn.ondes.ui.common.UiFormat;

/** Cells of the Passport grid: the stamps first, then any locked placeholders. */
public class StampAdapter extends RecyclerView.Adapter<StampAdapter.Holder> {

    // Ink colors and tilts repeat along the grid, so neighbours never look the same
    private static final int[] INKS = {R.color.stamp_1, R.color.stamp_2, R.color.stamp_3, R.color.stamp_4};
    private static final float[] TILTS = {-6, 4, -3, 7, -5, 3, -7, 5, -2, 6, -4, 2};

    private final List<StampEntity> stamps;
    private final int lockedCount;
    private final CountryCatalog catalog;

    public StampAdapter(List<StampEntity> stamps, int lockedCount, CountryCatalog catalog) {
        this.stamps = stamps;
        this.lockedCount = lockedCount;
        this.catalog = catalog;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View cell = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stamp, parent, false);
        return new Holder(cell);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (position < stamps.size()) {
            StampEntity stamp = stamps.get(position);
            bindStamp(holder, stamp.countryCode, catalog.nameOf(stamp.countryCode),
                    UiFormat.date(holder.itemView.getContext(), "dd MMM", new Date(stamp.firstDate)), position);
        } else {
            bindLocked(holder);
        }
    }

    @Override
    public int getItemCount() {
        return stamps.size() + lockedCount;
    }

    private static void bindStamp(Holder holder, String code, String name, String date, int position) {
        Context context = holder.itemView.getContext();
        Resources res = context.getResources();
        int ink = ContextCompat.getColor(context, INKS[position % INKS.length]);

        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setStroke(res.getDimensionPixelSize(R.dimen.stamp_stroke), ink);
        holder.ring.setBackground(ring);
        holder.ring.setRotation(TILTS[position % TILTS.length]);

        GradientDrawable innerRing = new GradientDrawable();
        innerRing.setShape(GradientDrawable.OVAL);
        innerRing.setStroke(res.getDimensionPixelSize(R.dimen.stroke_thin), ink,
                res.getDimension(R.dimen.dash_width), res.getDimension(R.dimen.dash_gap));
        holder.code.setBackground(innerRing);
        holder.code.setText(code);
        holder.code.setTextColor(ink);
        holder.code.setVisibility(View.VISIBLE);
        holder.lock.setVisibility(View.GONE);

        holder.name.setText(name);
        holder.name.setTextColor(ContextCompat.getColor(context, R.color.text));
        holder.date.setText(date);
        holder.date.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
        holder.date.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    private static void bindLocked(Holder holder) {
        Context context = holder.itemView.getContext();
        holder.ring.setBackgroundResource(R.drawable.bg_dashed_circle);
        holder.ring.setRotation(0);
        holder.code.setVisibility(View.GONE);
        holder.lock.setVisibility(View.VISIBLE);

        holder.name.setText(R.string.stamp_locked);
        holder.name.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
        holder.date.setText(R.string.stamp_locked_date);
        holder.date.setTextColor(ContextCompat.getColor(context, R.color.text_faint));
        // "Not yet" says it all, a screen reader would spell out the question marks
        holder.date.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View ring;
        final TextView code;
        final ImageView lock;
        final TextView name;
        final TextView date;

        Holder(View cell) {
            super(cell);
            ring = cell.findViewById(R.id.stamp_ring);
            code = cell.findViewById(R.id.stamp_code);
            lock = cell.findViewById(R.id.stamp_lock);
            name = cell.findViewById(R.id.stamp_name);
            date = cell.findViewById(R.id.stamp_date);
        }
    }
}
