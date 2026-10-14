package se.iths.erikthorell.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.iths.erikthorell.gateway.service.GatewayProxyService;

//Tar emot frontendens Http-anrop
@RestController
@RequestMapping("/gateway")
public class GatewayController {
    private final GatewayProxyService proxyService;

    public GatewayController(GatewayProxyService proxyService) {
        this.proxyService = proxyService;
    }
}