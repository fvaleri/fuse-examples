package it.fvaleri.integ.proxies;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import it.fvaleri.integ.config.ClientConfiguration;
import it.fvaleri.integ.models.Provider;

@FeignClient(name = "providers-service", url = "${microservice.providers.url}", configuration = {ClientConfiguration.class})
public interface ProviderProxy {
    @GetMapping("/providers/{id}")
    public Provider getDetails(@PathVariable("id") String id);

}
