package com.solop.sp013.lpe.support.nubefact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

/**
 * HTTP client for the NubeFact REST API
 * (<a href="https://www.nubefact.com">nubefact.com</a>, JSON integration manual).
 * Plays the same role as WebServiceHandler does for Invoicy (Uruguay) and AfipApiClient
 * does for Argentina: it only knows how to talk to the provider, the business logic lives
 * in NubeFact_v1.
 *
 * NubeFact exposes a single endpoint (the per-client RUTA). Every request is a POST to that
 * RUTA, the operation (generar_comprobante, consultar_comprobante, ...) travels in the body.
 * Authentication is a raw token sent in the Authorization header (no Bearer prefix).
 *
 * @author Gabriel Escalona
 */
public class NubeFactApiClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String route;
    private final String token;
    private BiConsumer<String, String> exchangeConsumer;

    public NubeFactApiClient(String route, String token) {
        this.route = route;
        this.token = token;
    }

    /**
     * Register a consumer that receives the raw (request, response) bodies of every call,
     * mirror of WebServiceHandler.xmlUsed() / AfipApiClient.onExchange()
     */
    public NubeFactApiClient onExchange(BiConsumer<String, String> exchangeConsumer) {
        this.exchangeConsumer = exchangeConsumer;
        return this;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * POST a JSON body to the NubeFact RUTA and return the parsed response.
     * NubeFact returns a meaningful JSON body even on HTTP errors ({ errors, codigo }),
     * so the body is parsed and returned instead of throwing: the caller inspects it.
     */
    public JsonNode post(ObjectNode body) {
        String request = null;
        String response = null;
        int statusCode = 0;
        try {
            request = mapper.writeValueAsString(body);
            HttpURLConnection connection = (HttpURLConnection) new URL(route).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", token);
            connection.setDoOutput(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(request.getBytes(StandardCharsets.UTF_8));
            }
            statusCode = connection.getResponseCode();
            InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            response = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if(exchangeConsumer != null) {
                exchangeConsumer.accept(request, response);
            }
            if(Util.isEmpty(response, true)) {
                throw new AdempiereException("NubeFact [" + statusCode + "] @NotFound@");
            }
            return mapper.readTree(response);
        } catch (IOException e) {
            if(exchangeConsumer != null) {
                exchangeConsumer.accept(request, response);
            }
            throw new AdempiereException(e);
        }
    }
}
