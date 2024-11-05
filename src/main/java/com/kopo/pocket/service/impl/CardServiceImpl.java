package com.kopo.pocket.service.impl;

import com.kopo.pocket.mapper.cards.CardMapper;
import com.kopo.pocket.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CardServiceImpl implements CardService {

    private final CardMapper cardMapper;

    @Autowired
    public CardServiceImpl(CardMapper cardMapper) {
        this.cardMapper = cardMapper;
    }

    @Override
    public List<String> getAllPackNames() {
        return cardMapper.getAllPackNames();
    }

    @Override
    public Map<String, List<Map<String, String>>> getCardImagesByPack() {
        List<Map<String, String>> cards = cardMapper.getCardImagesByPack();
        return cards.stream()
                .collect(Collectors.groupingBy(
                        card -> card.get("pack_name"),
                        Collectors.toList()
                ));
    }

    @Override
    public List<Map<String, Object>> getAllCardDetails() {
        List<Map<String, Object>> cards = cardMapper.getAllCardDetails();
        
        // section_names를 리스트로 변환
        for (Map<String, Object> card : cards) {
            String sectionNames = (String) card.get("section_names");
            if (sectionNames != null) {
                card.put("section_names", Arrays.asList(sectionNames.split(",")));
            } else {
                card.put("section_names", new ArrayList<>());
            }
        }
        
        return cards;
    }

    // @Override
    // public Map<String, Object> getCardDetailsByImageName(String imageName) {
    //     return cardMapper.getCardDetailsByImageName(imageName);
    // }
    @Override
    public Map<String, Object> getCardDetailsByImageName(String imageName) {
        List<Map<String, Object>> results = cardMapper.getCardDetailsByImageName(imageName);
        if (results.isEmpty()) {
            return null;
        }
        Map<String, Object> cardDetails = results.get(0);
        
        // Convert section_names from comma-separated string to list
        String sectionNames = (String) cardDetails.get("section_names");
        if (sectionNames != null) {
            cardDetails.put("section_names", Arrays.asList(sectionNames.split(",")));
        }
        
        // Parse movements only for Pokemon cards
        String movementsStr = (String) cardDetails.get("movements");
        if (movementsStr != null && !movementsStr.isEmpty()) {
            List<Map<String, String>> movements = Arrays.stream(movementsStr.split(";;"))
                .map(movement -> {
                    String[] parts = movement.split("\\|");
                    Map<String, String> moveMap = new HashMap<>();
                    moveMap.put("id", parts[0]);
                    moveMap.put("cost", parts[1]);
                    moveMap.put("move_name", parts[2]);
                    moveMap.put("damage", parts.length > 3 ? parts[3] : "");
                    moveMap.put("effect", parts.length > 4 ? parts[4] : "");
                    return moveMap;
                })
                .collect(Collectors.toList());
            cardDetails.put("movements", movements);
        } else {
            cardDetails.put("movements", new ArrayList<>());
        }
        
        return cardDetails;
    }

    @Override
    public Map<String, List<String>> getFilterOptions() {
        Map<String, List<String>> filterOptions = new HashMap<>();
        filterOptions.put("cardTypes", cardMapper.getCardTypes());
        filterOptions.put("pokemonTypes", cardMapper.getPokemonTypes());
        filterOptions.put("sets", cardMapper.getSets());
        filterOptions.put("sections", cardMapper.getSections());
        filterOptions.put("rarities", cardMapper.getRarities());
        return filterOptions;
    }
}