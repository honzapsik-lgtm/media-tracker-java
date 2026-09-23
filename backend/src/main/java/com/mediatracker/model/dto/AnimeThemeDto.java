package com.mediatracker.model.dto;

import java.util.ArrayList;
import java.util.List;

public class AnimeThemeDto {
    private List<String> openings = new ArrayList<>();
    private List<String> endings = new ArrayList<>();
    private List<AnimeThemeGroup> groups = new ArrayList<>();

    public static class AnimeThemeGroup {
        private String seasonName;
        private Integer seasonNumber;
        private Integer order;
        private List<String> openings = new ArrayList<>();
        private List<String> endings = new ArrayList<>();

        public AnimeThemeGroup() {}

        public AnimeThemeGroup(String seasonName, Integer seasonNumber, Integer order, List<String> openings, List<String> endings) {
            this.seasonName = seasonName;
            this.seasonNumber = seasonNumber;
            this.order = order;
            this.openings = openings != null ? openings : new ArrayList<>();
            this.endings = endings != null ? endings : new ArrayList<>();
        }

        public String getSeasonName() { return seasonName; }
        public void setSeasonName(String seasonName) { this.seasonName = seasonName; }

        public Integer getSeasonNumber() { return seasonNumber; }
        public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }

        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }

        public List<String> getOpenings() { return openings; }
        public void setOpenings(List<String> openings) { this.openings = openings; }

        public List<String> getEndings() { return endings; }
        public void setEndings(List<String> endings) { this.endings = endings; }
    }

    public AnimeThemeDto() {}

    public AnimeThemeDto(List<String> openings, List<String> endings) {
        this.openings = openings != null ? openings : new ArrayList<>();
        this.endings = endings != null ? endings : new ArrayList<>();
    }

    public List<String> getOpenings() { return openings; }
    public void setOpenings(List<String> openings) { this.openings = openings; }

    public List<String> getEndings() { return endings; }
    public void setEndings(List<String> endings) { this.endings = endings; }

    public List<AnimeThemeGroup> getGroups() { return groups; }
    public void setGroups(List<AnimeThemeGroup> groups) { this.groups = groups; }
}