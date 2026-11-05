package com.ironfit.backendmongo.ironbot.dto;

public record IronBotResponse(boolean disponible, String tipo, String titulo,
                              String mensaje, String modelo, String fuente) {}
