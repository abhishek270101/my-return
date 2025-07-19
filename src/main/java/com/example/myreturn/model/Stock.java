package com.example.myreturn.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stock")
@Getter
@Setter
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String exchange;

    @Column
    private String currency;

    @Column
    private String type;

    @Column
    private String country;

    @Column
    private String micCode;

    @Column
    private String figiCode;

    @Column
    private String cfiCode;

    @Column
    private String isin;

    @Column
    private String cusip;
} 