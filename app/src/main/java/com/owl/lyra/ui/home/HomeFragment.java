package com.owl.lyra.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.owl.lyra.R;

public class HomeFragment extends Fragment {

//    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        boolean spotify_auth_outcome = getActivity().getIntent().getExtras().getBoolean("spotify_auth_outcome");

        String welcomeText;

        if (spotify_auth_outcome) {
            welcomeText = "Spotify Authentication Successful";
        } else {
            welcomeText = "Spotify Authentication Failed";
        }


        TextView welcomeTextView = (TextView)root.findViewById(R.id.hm_welcome_text);
        welcomeTextView.setText(welcomeText);




        // View Model Stuff
//        homeViewModel =
//                new ViewModelProvider(this).get(HomeViewModel.class);
//        View root = inflater.inflate(R.layout.fragment_home, container, false);
//        final TextView textView = root.findViewById(R.id.hm_welcome_text);
//        homeViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });
        return root;
    }
}