package com.android.exttv.util;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.exttv.PlayerActivity;
import com.android.exttv.R;
import com.android.exttv.model.Program;
import com.android.exttv.model.ProgramDatabase;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.Map;

import static android.widget.Toast.LENGTH_SHORT;

public class RemoteKeyEvent {

    private final PlayerActivity playerActivity;
    private long leftKeyUpTime = 0;
    private long rightKeyUpTime = 0;
    private long backKeyUpTime = 0;
    private float leftAccelerator = 1;
    private float rightAccelerator = 1;
    private final boolean isLive;
    private long currentHash;

    public RemoteKeyEvent(PlayerActivity playerActivity, boolean isLive, long currentHash) {
        this.playerActivity = playerActivity;
        this.isLive = isLive;
        this.currentHash = currentHash;
    }

    private void makeLayuoutPopIn(int resourceId){
        Animation bottomUp = AnimationUtils.loadAnimation(playerActivity.getBaseContext(),
                R.anim.controls_pop_in);
        ViewGroup hiddenPanel = (ViewGroup) playerActivity.findViewById(resourceId);
        hiddenPanel.startAnimation(bottomUp);
        hiddenPanel.setVisibility(View.VISIBLE);
    }

    private void makeLayuoutPopOut(int resourceId){
        Animation bottomUp = AnimationUtils.loadAnimation(playerActivity.getBaseContext(),
                R.anim.controls_pop_out);
        ViewGroup hiddenPanel = (ViewGroup) playerActivity.findViewById(resourceId);
        hiddenPanel.startAnimation(bottomUp);
        hiddenPanel.setVisibility(View.INVISIBLE);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
//    Log.d("XXX", "key: "+action);
//    Log.d("XXX", "key: "+keyCode);

//        android.widget.TextView texto = playerActivity.findViewById(R.id.texto);
//        texto.setText(String.valueOf(keyCode));
        if (action == 1) {
            View bottomPanel = playerActivity.findViewById(R.id.bottom_panel);
            if (!isLive && playerActivity.cardsReady) {
                switch (keyCode) {
                    case 19: //up
                        if (!bottomPanel.isShown()) {
                            bottomPanel.setVisibility(View.VISIBLE);
                            makeLayuoutPopIn(R.id.bottom_panel);
                        }
                        return true;
                    case 4: //back
                    case 111: //back
                        if (!bottomPanel.isShown()) break;
                    case 20: //down
                        if (bottomPanel.isShown()) {
                            makeLayuoutPopOut(R.id.bottom_panel);
                            bottomPanel.setVisibility(View.INVISIBLE);
                            return true;
                        }else{
                            View hiddenPanel = playerActivity.findViewById(R.id.control_container);
                            if (!hiddenPanel.isShown()) {
                                hiddenPanel.setVisibility(View.VISIBLE);
                            }else{
                                hiddenPanel.setVisibility(View.INVISIBLE);
                            }
                        }
                        return true;
                    case 21: //left
                        if(bottomPanel.isShown()) {
                            playerActivity.scraper.displayerManager.selectPrevious();
                            return true;
                        }
                    case 22: //right
                        if(bottomPanel.isShown()) {
                            playerActivity.scraper.displayerManager.selectNext();
                            return true;
                        }
                    case 66: // enter
                    case 160: // enter
                        if (bottomPanel.isShown()) {
                            playerActivity.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                            playerActivity.scraper.displayerManager.playSelectedEpisode();
                            return true;
                        }
                    default:
                        // code block
                }
            }
        }
        if (action == 0) {
            switch (keyCode) {
                case 21: //left
                    if (System.currentTimeMillis() - leftKeyUpTime < 1000) leftAccelerator *= 1.25;
                    else leftAccelerator = 1;
                    break;
                case 22: //right
                    if (System.currentTimeMillis() - rightKeyUpTime < 1000)
                        rightAccelerator *= 1.25;
                    else rightAccelerator = 1;
                    break;
            }
        }
        if (action == 1) {
            switch (keyCode) {
                case 4: //back
                case 111: //back
                    if (System.currentTimeMillis() - backKeyUpTime < 3000) {
                        playerActivity.returnHomeScreen();
                    } else {
                        backKeyUpTime = System.currentTimeMillis();
                        Toast.makeText(playerActivity, "Press Back again to exit.", LENGTH_SHORT).show();
                    }
                    break;
                case 19: //up
                    if (isLive) {
//                        playerActivity.scraper.cancel();
                        Program next = null;
                        boolean breakNow = false;
                        for (Map.Entry<Integer, Program> e : ProgramDatabase.programs.entrySet()) {
                            if (breakNow) {
                                if (e.getValue().isLive()) {
                                    next = e.getValue();
                                    break;
                                }
                            } else if (e.getKey() == currentHash) {
                                breakNow = true;
                            }
                        }
                        if (next == null)
                            //get first
                            for (Map.Entry<Integer, Program> e : ProgramDatabase.programs.entrySet()) {
                                if (e.getValue().isLive()) {
                                    next = e.getValue();
                                    break;
                                }
                            }
                        ImageView watermark = playerActivity.findViewById(R.id.watermark);
                        watermark.setVisibility(View.VISIBLE);
                        Picasso.with(playerActivity.getBaseContext()).load(next.getLogo()).into((ImageView) playerActivity.findViewById(R.id.watermark));

                        ProgressBar progressBar = playerActivity.findViewById(R.id.progress_bar);
                        progressBar.setVisibility(View.VISIBLE);

                        playerActivity.startScraper(next);
                        currentHash = next.hashCode();
                    }
                    break;
                case 20: //down
                    if (isLive) {
//                        playerActivity.scraper.cancel();

                        Program previous = null;
                        //get last
                        for (Map.Entry<Integer, Program> e : ProgramDatabase.programs.entrySet()) {
                            if (e.getValue().isLive())
                                previous = e.getValue();
                        }
                        for (Map.Entry<Integer, Program> e : ProgramDatabase.programs.entrySet()) {
                            if (e.getKey() == currentHash) {
                                playerActivity.startScraper(previous);
                                currentHash = previous.hashCode();

                                ProgressBar progressBar = playerActivity.findViewById(R.id.progress_bar);
                                progressBar.setVisibility(View.VISIBLE);

                                ImageView watermark = playerActivity.findViewById(R.id.watermark);
                                watermark.setVisibility(View.VISIBLE);
                                Picasso.with(playerActivity.getBaseContext()).load(previous.getLogo()).into((ImageView) playerActivity.findViewById(R.id.watermark));

                                break;
                            }
                            if (e.getValue().isLive())
                                previous = e.getValue();
                        }
                    }
                    break;
                case 21: //left
                    if (!isLive) {
                        leftKeyUpTime = System.currentTimeMillis();
                        playerActivity.player.seekTo((long) (playerActivity.player.getCurrentPosition() - 30000 * leftAccelerator));
                    }
                    break;
                case 22: //right
                    if (!isLive) {
                        rightKeyUpTime = System.currentTimeMillis();
                        playerActivity.player.seekTo((long) (playerActivity.player.getCurrentPosition() + 30000 * rightAccelerator));
                    }
                    break;
                case 62:
                case 66: // enter
                case 160: // enter
                    playerActivity.player.setPlayWhenReady(!playerActivity.player.isPlaying());
                default:
                    // code block
            }
        }
        return true;
    }
}
