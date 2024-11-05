package com.kopo.pocket.mapper.cards;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CardMapper {

    @Select("SELECT DISTINCT pack_name FROM CARD")
    List<String> getAllPackNames();

    @Select("SELECT pack_name, image_name FROM CARD")
    List<Map<String, String>> getCardImagesByPack();


    List<Map<String, Object>> getAllCardDetails();
    List<Map<String, Object>> getCardDetailsByImageName(String imageName);

    @Select("SELECT DISTINCT card_type FROM CARD")
    List<String> getCardTypes();

    @Select("SELECT DISTINCT type FROM POKEMON_CARD")
    List<String> getPokemonTypes();

    @Select("SELECT pack_name FROM PACK")
    List<String> getSets();

    @Select("SELECT DISTINCT section_name FROM SECTION")
    List<String> getSections();

    @Select("SELECT DISTINCT rarity FROM CARD")
    List<String> getRarities();
}