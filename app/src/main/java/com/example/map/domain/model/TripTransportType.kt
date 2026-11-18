package com.example.map.domain.model

enum class TripTransportType(
    val apiValue: String,
    val label: String
) {
    CAR("Car", "Samochód"),
    PLANE("Plane", "Samolot"),
    TRAIN("Train", "Pociąg"),
    OTHER("Other", "Inny")
}