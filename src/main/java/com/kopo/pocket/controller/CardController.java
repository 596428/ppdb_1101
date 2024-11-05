package com.kopo.pocket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.kopo.pocket.service.CardService;

import org.springframework.web.bind.annotation.*;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;

import com.kopo.pocket.config.LanguageConfig;

import java.util.List;
import java.util.Map;

@Controller
public class CardController {

    private final CardService cardService;

    @Autowired
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }
    @Autowired
    private LanguageConfig.LanguageService languageService;

    @GetMapping("/")
    public String deckbuilder(Model model) {
        List<String> packNames = cardService.getAllPackNames();
        model.addAttribute("packNames", packNames);
        return "deckbuilder";
    }

    @GetMapping("/cardlist01")
    public String cardlist(Model model) {
        return "cardlist";
    }

    @GetMapping("/api/cards")
    @ResponseBody
    public List<Map<String, Object>> getAllCardDetails() {
        return cardService.getAllCardDetails();
    }

    @GetMapping("/api/card/{imageName}")
    @ResponseBody
    public Map<String, Object> getCardDetails(@PathVariable String imageName) {
        return cardService.getCardDetailsByImageName(imageName);
    }

    @GetMapping("/api/filter-options")
    public ResponseEntity<Map<String, List<String>>> getFilterOptions() {
        Map<String, List<String>> filterOptions = cardService.getFilterOptions();
        return ResponseEntity.ok(filterOptions);
    }

    @PostMapping("/api/log-download")
    @ResponseBody  // 전체 경로로 매핑
    public void logDownload() {
        // 로깅은 인터셉터에서 처리하므로 여기서는 아무것도 하지 않습니다
    }

    // @PostMapping("/language")
    // public ResponseEntity<String> setLanguage(@RequestBody LanguageRequest request) {
    //     LanguageConfig.Language language = LanguageConfig.Language.valueOf(request.getLanguage());
    //     languageService.setCurrentLanguage(language);
    //     return ResponseEntity.ok().body("Language updated to: " + language);
    // }

    // static class LanguageRequest {
    //     private String language;
        
    //     public String getLanguage() {
    //         return language;
    //     }
        
    //     public void setLanguage(String language) {
    //         this.language = language;
    //     }
    // }
}