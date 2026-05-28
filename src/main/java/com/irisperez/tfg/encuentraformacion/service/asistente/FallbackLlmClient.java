package com.irisperez.tfg.encuentraformacion.service.asistente;

import com.fasterxml.jackson.databind.JsonNode;
import com.irisperez.tfg.encuentraformacion.exception.AsistenteRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Primary
@Slf4j
@RequiredArgsConstructor
public class FallbackLlmClient implements LlmClient {

    private final GeminiClient gemini;
    private final GroqClient groq;

    private final AtomicLong geminiCooldownUntil = new AtomicLong(0);
    private final AtomicLong groqCooldownUntil = new AtomicLong(0);

    @Override
    public JsonNode llamar(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        boolean geminiDisponible = System.currentTimeMillis() >= geminiCooldownUntil.get();
        boolean groqDisponible = System.currentTimeMillis() >= groqCooldownUntil.get();

        if (geminiDisponible) {
            try {
                return gemini.llamar(messages, tools);
            } catch (AsistenteRateLimitException e) {
                long cooldownMs = (long) e.getRetryAfter() * 1000;
                geminiCooldownUntil.set(System.currentTimeMillis() + cooldownMs);
                log.warn("Gemini rate limit (Retry-After: {}s) — cooldown hasta {} — usando Groq",
                    e.getRetryAfter(), Instant.ofEpochMilli(geminiCooldownUntil.get()));
                groqDisponible = System.currentTimeMillis() >= groqCooldownUntil.get();
            }
        } else {
            log.debug("Gemini en cooldown — usando Groq directamente");
        }

        if (!groqDisponible) {
            log.warn("Groq también en cooldown hasta {} — ambos proveedores no disponibles",
                Instant.ofEpochMilli(groqCooldownUntil.get()));
            throw new AsistenteRateLimitException(
                (int) Math.max(1, (groqCooldownUntil.get() - System.currentTimeMillis()) / 1000));
        }

        try {
            return groq.llamar(messages, tools);
        } catch (AsistenteRateLimitException e) {
            long cooldownMs = (long) e.getRetryAfter() * 1000;
            groqCooldownUntil.set(System.currentTimeMillis() + cooldownMs);
            log.warn("Groq rate limit (Retry-After: {}s) — cooldown hasta {}",
                e.getRetryAfter(), Instant.ofEpochMilli(groqCooldownUntil.get()));
            throw e;
        }
    }
}
