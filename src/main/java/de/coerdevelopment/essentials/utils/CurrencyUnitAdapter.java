package de.coerdevelopment.essentials.utils;

import com.google.gson.*;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import java.lang.reflect.Type;

public final class CurrencyUnitAdapter
        implements JsonSerializer<CurrencyUnit>, JsonDeserializer<CurrencyUnit> {

    @Override
    public JsonElement serialize(CurrencyUnit src, Type typeOfSrc, JsonSerializationContext ctx) {
        return new JsonPrimitive(src.getCurrencyCode()); // z.B. "EUR"
    }

    @Override
    public CurrencyUnit deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        String code = json.getAsString();
        try {
            return Monetary.getCurrency(code); // holt Implementierung aus Moneta/JDK
        } catch (Exception e) {
            throw new JsonParseException("Unknown currency code: " + code, e);
        }
    }
}