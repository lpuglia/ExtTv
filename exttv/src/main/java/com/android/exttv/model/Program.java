package com.android.exttv.model;

import androidx.tvprovider.media.tv.TvContractCompat;

import java.util.Objects;

public class Program {
    private int posterArtAspectRatio = TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9;
    private String title;
    private String description;
    private String cardImageUrl;
    private String logo;
    private String videoUrl;
    private String type;
    private String scraperURL;
    private boolean requiresProxy=false;

    public String getScraperURL() {
        return scraperURL;
    }

    public Program setScraperURL(String scraperURL) {
        this.scraperURL = scraperURL;
        return this;
    }

    public String getLogo() {
        if(this.logo==null)
            return cardImageUrl;
        return logo;
    }

    public Program setLogo(String logo) {
        this.logo = logo;
        return this;
    }

    public int getPosterArtAspectRatio() {
        return posterArtAspectRatio;
    }

    public Program setPosterArtAspectRatio(int posterArtAspectRatio) {
        this.posterArtAspectRatio = posterArtAspectRatio;
        return this;
    }

    public boolean requiresProxy() {
        return requiresProxy;
    }

    public Program setRequireProxy(boolean requireProxy) {
        this.requiresProxy = requireProxy;
        return this;
    }

    public Program() {
    }

    public String getType() {
        return type;
    }

    public Program setType(String type) {
        this.type = type;
        return this;
    }

    public boolean isLive(){
        return type.equals("Live");
    }

    public String getTitle() {
        return title;
    }

    public Program setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Program setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public Program setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
        return this;
    }

    public String getCardImageUrl() {
        return cardImageUrl;
    }

    public Program setCardImageUrl(String cardImageUrl) {
        this.cardImageUrl = cardImageUrl;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Program program = (Program) o;
        return title.equals(program.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }

}