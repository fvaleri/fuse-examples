package it.fvaleri.integ;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService(targetNamespace = "http://it.fvaleri.integ/hello")
public interface HelloService {
    @WebMethod
    String writeText(String text);

}
