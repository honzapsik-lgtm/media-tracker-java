package com.mediatracker.model.dto;

public class MediaCreditDto {
    private String id;
    private String name;
    private String character;
    private String role;
    private String image;

    public MediaCreditDto() {}

    public MediaCreditDto(String id, String name, String character, String role, String image) {
        this.id = id;
        this.name = name;
        this.character = character;
        this.role = role;
        this.image = image;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}