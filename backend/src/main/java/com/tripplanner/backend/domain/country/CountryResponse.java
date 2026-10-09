package com.tripplanner.backend.domain.country;

public record CountryResponse(
        Integer countryId,
        String name,
        String currency,
        String voltage,
        String plugType,
        String imageUrl
) {
    public static CountryResponse from(Country country) {
        return new CountryResponse(
                country.getCountryId(),
                country.getName(),
                country.getCurrency(),
                country.getVoltage(),
                country.getPlugType(),
                country.getImageUrl()
        );
    }
}
