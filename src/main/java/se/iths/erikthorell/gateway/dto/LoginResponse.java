package se.iths.erikthorell.gateway.dto;

import java.util.List;

//denna dto ska skicka tillbaka till frontend, då vill vi ej ha token
public record LoginResponse(
        long expiresIn,
        String username,
        List<String> roles
) {
}
