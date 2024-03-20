package com.owl.lyra;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.owl.lyra.services.LoggingService;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

public class RecyclerAdaptor extends RecyclerView.Adapter<RecyclerAdaptor.CustomViewHolder> {

    private ArrayList<DownloadSongCard> mTrackList;

    public static class CustomViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        public ImageView mthumbnail;
        public TextView mTitle;
        public TextView mArtists;
        public TextView mAlbum;
        public ImageButton mLinkPopup;
        public TextView mVariation;
        public ImageView mStatus;

        //View holder constructor
        public CustomViewHolder(@NonNull View itemView) {
            super(itemView);
            mthumbnail = itemView.findViewById(R.id.thumbnail);
            mTitle = itemView.findViewById(R.id.track_title);
            mArtists = itemView.findViewById(R.id.track_artists);
            mAlbum = itemView.findViewById(R.id.track_album);
            mLinkPopup = itemView.findViewById(R.id.link_popup_btn);
            mVariation = itemView.findViewById(R.id.variation);
            mStatus = itemView.findViewById(R.id.status_icon);

            itemView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("Select The Action");
            v.setLongClickable(true);
            menu.add(this.getAdapterPosition(), v.getId(), 0, "Change Link");
//            menu.add(this.getAdapterPosition(), v.getId(), 0, "Do something");
        }
    }

    //Adaptor Constructor, passing in the data array
    public RecyclerAdaptor(ArrayList<DownloadSongCard> trackList) {
        mTrackList = trackList;
    }

    @NonNull
    @Override
    public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.dl_song_card, parent, false);
        CustomViewHolder vh = new CustomViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
        DownloadSongCard currentItem = mTrackList.get(position);

        //Loading image into thumbnail ImageView via Picasso
        Picasso.get().load(currentItem.getAlbumArtURL()).into(holder.mthumbnail);

        //Setting the remaining track information
        holder.mTitle.setText(currentItem.getTrackTitle());
        holder.mArtists.setText(currentItem.getTrackArtists());
        holder.mAlbum.setText(currentItem.getAlbum());

        // Setting the status
        switch(currentItem.getStatus()) {
            case "searching":
                holder.mStatus.setImageResource(R.drawable.ic_searching);
                holder.mVariation.setAlpha(0);
                holder.mLinkPopup.setImageAlpha(0);
                break;

            case "awaiting_dl":
                holder.mStatus.setImageResource(R.drawable.checkmark);
                float raw_variation = Float.parseFloat(String.format(Locale.ENGLISH, "%.2f",((double)currentItem.getVariation())/1000));
                String variation_seconds = raw_variation + "s";
                LoggingService.Logger.addRecordToLog("VARIATION SECONDS" + variation_seconds);
                holder.mVariation.setText(variation_seconds);
                // Determine colour
                if (raw_variation <= 1.5) {
                    // Very good
                    holder.mVariation.setTextColor(Color.parseColor("#00f05c"));
                }
                else if (raw_variation > 1.5 && raw_variation <= 2) {
                    // Good
                    holder.mVariation.setTextColor(Color.parseColor("#90ff6e"));
                }
                else if (raw_variation > 2 && raw_variation <= 3) {
                    // Medium Confidence
                    holder.mVariation.setTextColor(Color.parseColor("#f3ff4a"));
                }
                else if (raw_variation > 3 && raw_variation <= 5) {
                    // Poor
                    holder.mVariation.setTextColor(Color.parseColor("#ff591c"));
                }
                else {
                    // Very Poor
                    holder.mVariation.setTextColor(Color.parseColor("#fc4242"));
                }



                holder.mVariation.setAlpha(1);
                holder.mLinkPopup.setOnClickListener(
                        view -> {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.setData(Uri.parse(currentItem.getVideoURL()));
                            view.getContext().startActivity(intent);
                        }
                );
                holder.mLinkPopup.setImageAlpha(255);
                break;

            case "search_failed":
                holder.mStatus.setImageResource(R.drawable.ic_failed);
                holder.mVariation.setAlpha(0);
                holder.mLinkPopup.setImageAlpha(0);
                break;

            case "download_queued":
                holder.mStatus.setImageResource(R.drawable.ic_downloadicon);
                break;

            case "link_extraction_failed":
                holder.mStatus.setImageResource(R.drawable.ic_failed);
                break;

            case "converting":
                holder.mStatus.setImageResource(R.drawable.ic_conversion);
                break;

            case "conversion_failed":
                holder.mStatus.setImageResource(R.drawable.ic_conversion_failed);
                break;

            case "metadata_assignment_failure":
                holder.mStatus.setImageResource(R.drawable.ic_no_metadata);
                break;

            case "success":
                holder.mStatus.setImageResource(R.drawable.ic_double_tick);
                break;
        }

    }



    @Override
    public int getItemCount() {
        return mTrackList.size();
    }
}
