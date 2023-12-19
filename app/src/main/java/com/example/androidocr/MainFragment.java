package com.example.androidocr;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import java.io.File;

public class MainFragment extends Fragment {
    private View rootView;

    private MainViewModel viewModel;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.init();

        // Copy sample image and language data to storage
        Assets.extractAssets(requireContext());

        if (!viewModel.isInitialized()) {
            String dataPath = Assets.getTessDataPath(requireContext());
            viewModel.initTesseract(dataPath, Config.TESS_LANG, Config.TESS_ENGINE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView =getLayoutInflater().inflate(R.layout.fragment_main, null);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ImageView)rootView.findViewById(R.id.image)).setImageBitmap(Assets.getImageBitmap(requireContext()));
        ((Button)rootView.findViewById(R.id.start)).setOnClickListener(v -> {
            File imageFile = Assets.getImageFile(requireContext());
            viewModel.recognizeImage(imageFile);
        });
        ((Button)rootView.findViewById(R.id.stop)).setOnClickListener(v -> {
            viewModel.stop();
        });
        ((TextView)rootView.findViewById(R.id.text)).setMovementMethod(new ScrollingMovementMethod());

        viewModel.getProcessing().observe(getViewLifecycleOwner(), processing -> {
            ((Button)rootView.findViewById(R.id.start)).setEnabled(!processing);
            ((Button)rootView.findViewById(R.id.stop)).setEnabled(processing);
        });
        viewModel.getProgress().observe(getViewLifecycleOwner(), progress -> {
            ((TextView)rootView.findViewById(R.id.status)).setText(progress);
        });
        viewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            ((TextView)rootView.findViewById(R.id.text)).setText(result);
        });
    }
}