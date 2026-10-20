package se.iths.erikthorell.gateway.dto;

import java.util.List;

//denna dto ska göra om JSON från auth till ett java objekt
public record AuthServiceResponse(
        String accessToken,
        long expiresIn,
        String username,
        List<String> roles
) {
}
