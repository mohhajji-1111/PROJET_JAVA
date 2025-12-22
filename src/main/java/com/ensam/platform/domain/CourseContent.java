package com.ensam.platform.domain;

import jakarta.persistence.*;

@Embeddable
public class CourseContent {

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String text;

    public CourseContent() {
    }

    public CourseContent(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
