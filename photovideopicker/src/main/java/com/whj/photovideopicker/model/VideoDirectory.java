package com.whj.photovideopicker.model;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class VideoDirectory{

    private String id;
    private String coverPath;
    private String name;
    private long dateAdded;
    private List<Video> videos = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoDirectory)) return false;

        VideoDirectory directory = (VideoDirectory) o;

        if (!id.equals(directory.id)) return false;

        if(TextUtils.isEmpty(name)){
            return false;
        }

        return name.equals(directory.name);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public List<Video> getVideos() {
        return videos;
    }

    public void setVideos(List<Video> photos) {
        this.videos = photos;
    }

    public void addVideo(int id, String path, Long duration, Long size , String name) {
        videos.add(new Video(id, path, duration,size,name));
    }

    public List<String> getVideoPaths() {
        List<String> paths = new ArrayList<>(videos.size());
        for (Video video : videos) {
            paths.add(video.getPath());
        }
        return paths;
    }

}
