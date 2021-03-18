package com.android.exttv.scrapers;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.android.exttv.PlayerActivity;
import com.android.exttv.R;
import com.android.exttv.model.Episode;
import com.squareup.picasso.Picasso;

import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

public class DisplayerManager {

    private final PlayerActivity playerActivity;
    private final ScraperManager scraperManager;
    private ItemArrayAdapter itemArrayAdapter;
    private RecyclerView.LayoutManager layoutManager;

    public DisplayerManager(PlayerActivity playerActivity, ScraperManager scraperManager, boolean isLive) {
        this.playerActivity = playerActivity;
        this.scraperManager = scraperManager;
        if(!isLive){
            // Initializing list view with the custom adapter
            layoutManager = new LinearLayoutManager(playerActivity.getBaseContext(), LinearLayoutManager.HORIZONTAL, false);
            itemArrayAdapter = new ItemArrayAdapter();
            RecyclerView listView = (RecyclerView) playerActivity.findViewById(R.id.episode_list);
            listView.setLayoutManager(layoutManager);
            listView.setItemAnimator(new DefaultItemAnimator());
            listView.setAdapter(itemArrayAdapter);
            SimpleItemAnimator simpleItemAnimator = ((SimpleItemAnimator) listView.getItemAnimator());
            simpleItemAnimator.setSupportsChangeAnimations(false);
            simpleItemAnimator.setMoveDuration(0);
            simpleItemAnimator.setAddDuration(0);
            simpleItemAnimator.setChangeDuration(0);
            simpleItemAnimator.setRemoveDuration(0);
        }
    }

    public void setTopContainer(Episode episode){
        ImageView thumb = playerActivity.findViewById(R.id.top_thumb);
        Picasso.with(playerActivity.getBaseContext())
                .load(episode.getThumbURL())
                .into(thumb, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        playerActivity.findViewById(R.id.top_thumb_progress).setVisibility(View.GONE);
                    }
                    @Override
                    public void onError() {}
                });
        TextView title = playerActivity.findViewById(R.id.top_title);
        title.setText(episode.getTitle());
        TextView date = playerActivity.findViewById(R.id.top_date);
        date.setText(episode.getAirDate().toZonedDateTime().format(DateTimeFormatter.ofPattern("d MMM uuuu")));
        TextView description = playerActivity.findViewById(R.id.top_description);
        description.setText(episode.getDescription());
    }

    public void selectPrevious() {
        layoutManager.scrollToPosition(itemArrayAdapter.selectPrevious());
    }

    public void selectNext() {
        layoutManager.scrollToPosition(itemArrayAdapter.selectNext());
    }

    public void playSelectedEpisode() {
        scraperManager.handleEpisode(itemArrayAdapter.getFocusedEpisode(), true);
    }

    LinkedList<String> messages = new LinkedList<>();

    protected void runOnUiThread(Runnable runnable){
        playerActivity.runOnUiThread(runnable);
    }

    protected void toastOnUi(String message){
        runOnUiThread(() -> {
            StringBuilder full_message = new StringBuilder();
            for(String m : messages) full_message.append("> ").append(m).append("\n");

            int length = Math.min(message.length(), 45);
            messages.addLast( message.length()<47 ? message : message.substring(0, length)+"...");
            full_message.append("> ").append(messages.get(messages.size() - 1));
            Toast toast = Toast.makeText(playerActivity, full_message.toString(), Toast.LENGTH_LONG);
            toast.show();
            if(messages.size()>5) messages.removeFirst();
        });
    }

    public void addData(Episode episode) {
        itemArrayAdapter.addData(episode);
    }

}
