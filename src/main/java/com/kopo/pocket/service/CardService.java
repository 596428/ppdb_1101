package com.kopo.pocket.service;

import java.util.List;
import java.util.Map;

public interface CardService {
    List<String> getAllPackNames();
    public Map<String, List<Map<String, String>>> getCardImagesByPack();

    List<Map<String, Object>> getAllCardDetails();
    Map<String, Object> getCardDetailsByImageName(String imageName);

    Map<String, List<String>> getFilterOptions();
}