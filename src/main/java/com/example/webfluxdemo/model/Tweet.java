package com.example.webfluxdemo.model;

import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tweets")
public class Tweet {
    
    @Id
    private String id;
    
    @NotBlank
    @Size(max = 140)
    private String text;
    
    @NotNull
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public Tweet() {
    }
    
    public Tweet(String text) {
        this.text = text;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Tweet [id=" + id + ", text=" + text + ", createdAt=" + createdAt + "]";
    }

}
