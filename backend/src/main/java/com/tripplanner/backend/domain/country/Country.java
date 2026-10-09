package com.tripplanner.backend.domain.country;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "country")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Country {

    @Id
    private Integer countryId;

    private String name;

    private String currency;

    private String voltage;

    private String plugType;

    private Boolean isActive;

    private Integer sortOrder;

    private String imageUrl;

    private String routingProvider;

    private String placeProvider;
}
