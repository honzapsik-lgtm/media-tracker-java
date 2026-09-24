package com.mediatracker.model.dto;

public class DiscoverItemDto {
    private String id;
    private String title;
    private String image;
    private String type;
    private Integer globalScore;
    private Integer communityScore;
    private Integer listRank;
    private String releaseDate;

    public DiscoverItemDto() {}

    public DiscoverItemDto(String id, String title, String image, String type, Integer globalScore, String releaseDate) {
        this.id = id;
        this.title = title;
        this.image = image;
        this.type = type;
        this.globalScore = globalScore;
        this.releaseDate = releaseDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getGlobalScore() { return globalScore; }
    public void setGlobalScore(Integer globalScore) { this.globalScore = globalScore; }
    public Integer getCommunityScore() { return communityScore; }
    public void setCommunityScore(Integer communityScore) { this.communityScore = communityScore; }
    public Integer getListRank() { return listRank; }
    public void setListRank(Integer listRank) { this.listRank = listRank; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
}