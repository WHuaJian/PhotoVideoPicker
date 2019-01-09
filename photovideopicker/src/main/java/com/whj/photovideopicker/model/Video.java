package com.whj.photovideopicker.model;

import java.io.Serializable;

/**
 *
 */
public class Video implements Serializable {

    private int id;
    private Long size;
    private String path;
    private String name;
    private long duration;

    public Video(int id, String path, Long duration, Long size, String name) {
        this.id = id;
        this.path = path;
        this.duration = duration;
        this.size = size;
        this.name =name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Video)) return false;

        Video video = (Video) o;

        return id == video.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
