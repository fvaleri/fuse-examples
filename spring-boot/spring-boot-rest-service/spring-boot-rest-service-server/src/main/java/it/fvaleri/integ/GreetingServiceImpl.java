package it.fvaleri.integ;

import org.apache.camel.Header;
import org.springframework.stereotype.Service;

@Service("greetingService")
public class GreetingServiceImpl implements GreetingService {
    @Override
    public Greeting getGreeting(@Header("name") String name) {
        return new Greeting("Hello " + name);
    }
}

