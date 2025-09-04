package de.coerdevelopment.essentials.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.javamoney.moneta.Money;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.io.IOException;
import java.math.BigDecimal;

public class MonetaryAmountAdapter extends TypeAdapter<MonetaryAmount> {

    @Override
    public void write(JsonWriter out, MonetaryAmount value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginObject();
        out.name("amount").value(value.getNumber().numberValueExact(BigDecimal.class));
        out.name("currency").value(value.getCurrency().getCurrencyCode());
        out.endObject();
    }

    @Override
    public MonetaryAmount read(JsonReader in) throws IOException {
        BigDecimal amount = null;
        String currency = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "amount":
                    amount = new BigDecimal(in.nextString());
                    break;
                case "currency":
                    currency = in.nextString();
                    break;
            }
        }
        in.endObject();

        if (amount != null && currency != null) {
            return Money.of(amount, Monetary.getCurrency(currency));
        }
        return null;
    }
}
