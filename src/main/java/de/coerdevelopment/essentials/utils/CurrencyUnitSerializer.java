package de.coerdevelopment.essentials.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import javax.money.CurrencyUnit;
import java.io.IOException;

public class CurrencyUnitSerializer extends StdSerializer<CurrencyUnit> {
    public CurrencyUnitSerializer() {
        super(CurrencyUnit.class);
    }

    @Override
    public void serialize(CurrencyUnit value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getCurrencyCode());
    }
}