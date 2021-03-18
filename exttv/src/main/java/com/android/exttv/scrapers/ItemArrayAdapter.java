package com.android.exttv.scrapers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.exttv.R;
import com.android.exttv.model.Episode;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

public class ItemArrayAdapter extends RecyclerView.Adapter<ItemArrayAdapter.ViewHolder> {

    private static class EpisodesArraySorted {
        private final ArrayList<Episode> episodes = new ArrayList<>();
        public int add(Episode p){
            int index = Collections.binarySearch(episodes, p);
            if (index < 0) index = ~index;
            if(index-1>=0 && index< episodes.size() && episodes.get(index-1).getPageURL().equals(p.getPageURL()))
                episodes.set(index-1, p);
            else if(index< episodes.size() && episodes.get(index).getPageURL().equals(p.getPageURL()))
                episodes.set(index, p);
            else
                episodes.add(index, p);
            return index;
        }
        public Episode get(int index){ return episodes.get(index); }
        public int length(){ return  episodes.size(); }
    }

    private final EpisodesArraySorted episodeList = new EpisodesArraySorted();
    private final ArrayList<View> viewList = new ArrayList<>();
    private final ArrayList<Target> targets = new ArrayList<>();
    private Context context;
    private int focusedItem = 0;

    @Override
    public int getItemCount() {
        return episodeList == null ? 0 : episodeList.episodes.size();
    }
    public Episode getFocusedEpisode(){ return episodeList.get(focusedItem); }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        // Inflate the custom layout
        viewList.add(inflater.inflate(R.layout.activity_listview, parent, false));

        return new ViewHolder(viewList.get(viewList.size() - 1));
    }

    public void addData(Episode episode) {
        int index = episodeList.add(episode);
        notifyItemRangeChanged(Math.min(index-1,0), Math.max(episodeList.length()-1,0));
    }

    public int selectNext(){
        if(focusedItem<episodeList.episodes.size()-1){
            notifyItemChanged(focusedItem);
            focusedItem+=1;
            notifyItemChanged(focusedItem);
        }
        return Math.min(focusedItem + 2, episodeList.episodes.size()-1);
    }

    public int selectPrevious(){
        if(focusedItem>0) {
            notifyItemChanged(focusedItem);
            focusedItem -= 1;
            notifyItemChanged(focusedItem);
        }
        return Math.max(focusedItem - 2,0);
    }

    // load data in each row element
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int listPosition) {
        Episode episode = episodeList.get(listPosition);
        int white = Color.parseColor("#FFFFFF");
        holder.title.setTextColor(white);
        holder.title.setText(episode.getTitle());
        holder.date.setTextColor(white);
        holder.date.setText(episode.getAirDate().toZonedDateTime().format(DateTimeFormatter.ofPattern("d MMM uuuu")));
        holder.duration.setTextColor(white);
        holder.duration.setText(" ⬩ " + episode.getDuration());
        holder.description.setTextColor(white);
        holder.description.setText(episode.getDescription());

        if(episode.getThumb()!=null){
            holder.progress.setVisibility(View.GONE);
            holder.thumbnail.setImageBitmap(episode.getThumb());
        }else{

            targets.add(new Target() {
                @Override
                public void onBitmapLoaded (final Bitmap bitmap, Picasso.LoadedFrom from) {
                    holder.progress.setVisibility(View.GONE);
                    holder.thumbnail.setImageBitmap(bitmap);
                    episode.setThumb(bitmap);
                }
                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {}

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {}
            });

            Picasso.with(context)
                    .load(episode.getThumbURL())
                    .into(targets.get(targets.size()-1));
        }

        holder.card.setBackgroundColor(Color.parseColor("#CC303030"));
        holder.container.setPadding(10,10,10,10);
        if(listPosition==focusedItem){
            holder.card.setBackgroundColor(Color.parseColor("#FFCCCCCC"));
            holder.container.setPadding(0,0,0,0);
            int black = Color.parseColor("#000000");
            holder.title.setTextColor(black);
            holder.date.setTextColor(black);
            holder.duration.setTextColor(black);
            holder.description.setTextColor(black);
        }
    }


    // Static inner class to initialize the views of rows
    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView title;
        public TextView date;
        public TextView duration;
        public TextView description;
        public ImageView thumbnail;
        public LinearLayout card;
        public LinearLayout container;
        public ProgressBar progress;
        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            date = (TextView) itemView.findViewById(R.id.date);
            duration = (TextView) itemView.findViewById(R.id.duration);
            description = (TextView) itemView.findViewById(R.id.description);
            thumbnail = (ImageView) itemView.findViewById(R.id.thumb);
            card = (LinearLayout) itemView.findViewById(R.id.episode_card);
            container = (LinearLayout) itemView.findViewById(R.id.episode_container);
            progress = (ProgressBar) itemView.findViewById(R.id.cards_progress);
        }

    }

}