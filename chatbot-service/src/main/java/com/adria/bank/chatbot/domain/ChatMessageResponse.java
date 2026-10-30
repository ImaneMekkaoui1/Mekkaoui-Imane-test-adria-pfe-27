package com.adria.bank.chatbot.domain;

public record ChatMessageResponse(
        String sessionId,
        String reply,
        String executedTool,
        Object toolResult
) {}

