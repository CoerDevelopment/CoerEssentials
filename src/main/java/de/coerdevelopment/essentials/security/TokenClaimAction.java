package de.coerdevelopment.essentials.security;

import java.util.Map;

public abstract class TokenClaimAction {

    public abstract void addClaim(Map<String, Object> claims);

}
