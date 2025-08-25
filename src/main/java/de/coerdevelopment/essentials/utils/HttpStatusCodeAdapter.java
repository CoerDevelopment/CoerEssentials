package de.coerdevelopment.essentials.utils;

import com.google.gson.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.lang.reflect.Type;

public final class HttpStatusCodeAdapter implements JsonSerializer<HttpStatusCode>, JsonDeserializer<HttpStatusCode> {

    @Override
    public JsonElement serialize(HttpStatusCode src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.value());
    }

    @Override
    public HttpStatusCode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        int code = json.getAsInt();
        try {
            return HttpStatusCode.valueOf(code);
        } catch (NoSuchMethodError e) {
            return HttpStatus.valueOf(code);
        }
    }
}
