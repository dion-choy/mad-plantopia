package com.sp.madproj;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sp.madproj.Identify.IdentificationHelper;
import com.sp.madproj.Plant.PlantHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class BadgeFrag extends Fragment {
    private final List<String> achievedModel= new ArrayList<>();
    private final List<String> inProgModel = new ArrayList<>();

    public BadgeFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = requireContext();
        if (new File(context.getApplicationContext().getFilesDir(), "localBadges.txt").exists()) {
            return;
        }
        InputStream assetIs;
        OutputStream copyOs;

        try {
            assetIs = context.getAssets().open("badges.txt");
            copyOs = context.openFileOutput("localBadges.txt", MODE_PRIVATE);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = assetIs.read(buffer)) != -1) {
                copyOs.write(buffer, 0, bytesRead);
            }

            assetIs.close();
            copyOs.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_badge, container, false);

        BadgesAdapter achieved = new BadgesAdapter(achievedModel, true);

        RecyclerView achievedList = view.findViewById(R.id.achievedList);
        achievedList.setHasFixedSize(true);
        achievedList.setLayoutManager(new LinearLayoutManager(getContext()));
        achievedList.setItemAnimator(new DefaultItemAnimator());
        achievedList.setAdapter(achieved);

        BadgesAdapter inProg = new BadgesAdapter(inProgModel, false);
        RecyclerView inProgList = view.findViewById(R.id.inProgressList);
        inProgList.setHasFixedSize(true);
        inProgList.setLayoutManager(new LinearLayoutManager(getContext()));
        inProgList.setItemAnimator(new DefaultItemAnimator());
        inProgList.setAdapter(inProg);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        achievedModel.clear();
        inProgModel.clear();

        StringBuilder updatedBadges = new StringBuilder();

        FileInputStream input;
        try {
            input = new FileInputStream(
                    new File(requireContext().getApplicationContext().getFilesDir(), "localBadges.txt")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        int lineCount = 0;
        while(true) {
            try {
                if (!reader.ready()) {
                    break;
                }
                String line = reader.readLine();
                String[] badge = line.split("\\|");
                if (badge[1].equals("0")) {
                    inProgModel.add(badge[0]);
                } else {
                    achievedModel.add(badge[0]);
                }
                Log.d("Line", line);

                updatedBadges.append(checkBadge(lineCount, line))
                        .append("\n");
                lineCount++;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    requireContext().openFileOutput("localBadges.txt", MODE_PRIVATE)
            );
            outputStreamWriter.write(updatedBadges.toString());
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e);
        }
    }

    private String checkBadge(int lineCount, String initLine) {
        StringBuilder line = new StringBuilder(initLine);
        if (line.charAt(line.length()-1) == '1') {
            return initLine;
        }

        PlantHelper plantHelper = new PlantHelper(requireContext());
        IdentificationHelper identificationHelper = new IdentificationHelper(requireContext());
        switch (lineCount) {
            case 0:
                if (plantHelper.getAll().getCount() > 0) {
                    line.setCharAt(line.length()-1, '1');
                }
                break;
            case 1:
                if (plantHelper.getAll().getCount() == 11) {
                    line.setCharAt(line.length()-1, '1');
                }
                break;
            case 2:
                if (identificationHelper.getAll().getCount() > 0) {
                    line.setCharAt(line.length()-1, '1');
                }
                break;
            case 3:
                SharedPreferences sharedPref = requireActivity().getSharedPreferences("greenhouse", MODE_PRIVATE);
                if(sharedPref.getBoolean("joinedRoom", false)) {
                    line.setCharAt(line.length()-1, '1');
                }
            default:
                break;
        }
        plantHelper.close();
        identificationHelper.close();

        return line.toString();
    }

    private class BadgesAdapter extends RecyclerView.Adapter<BadgesAdapter.BadgesHolder>{
        private final List<String> badges;
        private final boolean achieved;

        BadgesAdapter(List<String> badges, boolean achieved) {
            this.badges = badges;
            this.achieved = achieved;
        }

        @NonNull
        @Override
        public BadgesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_badges, parent, false);
            return new BadgesHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BadgesHolder holder, final int position) {
            String[] badgeInfo = badges.get(position).split(";");
            holder.title.setText(badgeInfo[0]);
            holder.description.setText(badgeInfo[1]);

            if (achieved) {
                holder.inProgShade.setVisibility(View.GONE);
            } else {
                holder.inProgShade.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return badges.size();
        }

        class BadgesHolder extends RecyclerView.ViewHolder {
            private final TextView title;
            private final TextView description;
            private final View inProgShade;

            public BadgesHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.title);
                description = itemView.findViewById(R.id.description);
                inProgShade =  itemView.findViewById(R.id.inProgShade);
            }
        }
    }
}