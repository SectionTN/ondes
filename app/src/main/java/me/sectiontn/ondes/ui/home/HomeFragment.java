package me.sectiontn.ondes.ui.home;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import me.sectiontn.ondes.R;
import me.sectiontn.ondes.ui.game.GameActivity;

/** The Play tab: today's daily challenge, free play and a shortcut to Explore. */
public class HomeFragment extends Fragment {

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.play_button).setOnClickListener(v -> startGame(GameActivity.MODE_DAILY));
        view.findViewById(R.id.free_play_button).setOnClickListener(v -> startGame(GameActivity.MODE_FREE_PLAY));
    }

    private void startGame(String mode) {
        startActivity(GameActivity.newIntent(requireContext(), mode));
    }
}
