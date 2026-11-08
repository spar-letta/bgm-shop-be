package com.app.itibari.models.entities;

import com.app.itibari.dataTypes.CounterType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Random;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "counter", schema = "itibari")
public class Counter implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "counter_type")
    private CounterType counterType;

    @Column(name = "counter_current")
    private Integer current;

    public Counter(CounterType counterType) {

        this.counterType = counterType;
        this.current = 100000;                 //for testing Tra receipt --> this.current = 42; next add 1
    }
}
