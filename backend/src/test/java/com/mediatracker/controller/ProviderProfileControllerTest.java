package com.mediatracker.controller;

import com.mediatracker.model.dto.UnifiedProfileDto;
import com.mediatracker.model.dto.UnifiedCompanyProfileDto;
import com.mediatracker.service.PersonProfileService;
import com.mediatracker.service.CompanyProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProviderProfileControllerTest {
    @Test
    void personEndpointSerializesContractAndReturns404ForMissing() throws Exception {
        var service = mock(PersonProfileService.class);
        var credit = new UnifiedProfileDto.Credit("tmdb-movie-2", "MOVIE", "Film", null, 2020, "Hero", true, null);
        when(service.getProfile("tmdb-1")).thenReturn(Optional.of(new UnifiedProfileDto("tmdb-1", 1, null, null, null, null,
                null, null, "Person", null, null, null, null, null, null,
                new UnifiedProfileDto.Credits(List.of(credit), List.of()))));
        var mvc = MockMvcBuilders.standaloneSetup(new PersonProfileController(service)).build();
        mvc.perform(get("/api/person/tmdb-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tmdb-1"))
                .andExpect(jsonPath("$.credits.cast[0].isVoiceRole").value(true))
                .andExpect(jsonPath("$.credits.crew").isArray());
        mvc.perform(get("/api/person/missing")).andExpect(status().isNotFound());
    }

    @Test
    void companyEndpointSerializesAllPortfolioBuckets() throws Exception {
        var service = mock(CompanyProfileService.class);
        when(service.getProfile("igdb-1")).thenReturn(Optional.of(new UnifiedCompanyProfileDto("igdb-1", "Company", null, null, null,
                new UnifiedCompanyProfileDto.Portfolio(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))));
        var mvc = MockMvcBuilders.standaloneSetup(new CompanyProfileController(service)).build();
        mvc.perform(get("/api/company/igdb-1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolio.developedGames").isArray())
                .andExpect(jsonPath("$.portfolio.publishedGames").isArray())
                .andExpect(jsonPath("$.portfolio.animationStudioFor").isArray())
                .andExpect(jsonPath("$.portfolio.producedAnime").isArray())
                .andExpect(jsonPath("$.portfolio.publishedManga").isArray())
                .andExpect(jsonPath("$.portfolio.producedFilmTv").isArray())
                .andExpect(jsonPath("$.portfolio.broadcastedOn").isArray());
        mvc.perform(get("/api/company/missing")).andExpect(status().isNotFound());
    }
}
