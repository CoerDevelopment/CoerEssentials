package de.coerdevelopment.essentials.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import javax.money.MonetaryAmount;
import java.io.IOException;

public class MonetaryAmountSerializer extends StdSerializer<MonetaryAmount> {

    public MonetaryAmountSerializer() {
        super(MonetaryAmount.class);
    }

    @Override
    public void serialize(MonetaryAmount value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("amount", value.getNumber().numberValueExact(Double.class));
        gen.writeStringField("currency", value.getCurrency().getCurrencyCode());
        gen.writeEndObject();
    }
}