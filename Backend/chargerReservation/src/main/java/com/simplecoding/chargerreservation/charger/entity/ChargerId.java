package com.simplecoding.chargerreservation.charger.entity;
// 복합키 때문에 만든 entity
import java.io.Serializable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChargerId implements Serializable {
    private String statId;
    private String chargerId;
}