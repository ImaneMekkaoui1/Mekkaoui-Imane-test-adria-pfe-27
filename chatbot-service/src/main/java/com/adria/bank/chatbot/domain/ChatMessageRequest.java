package com.adria.bank.chatbot.domain;

public record ChatMessageRequest(
        String message,
        String sessionId
) {}

