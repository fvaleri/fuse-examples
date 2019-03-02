package it.fvaleri.integ;

import org.tempuri.Add;
import org.tempuri.Divide;
import org.tempuri.Multiply;
import org.tempuri.Subtract;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Instruction {
    @JsonProperty
    Add add;

    @JsonProperty
    Divide divide;

    @JsonProperty
    Multiply multiply;

    @JsonProperty
    Subtract subtract;

    Object operation() {
        if (add != null) {
            return add;
        } else if (divide != null) {
            return divide;
        } else if (multiply != null) {
            return multiply;
        } else {
            return subtract;
        }
    }

    String soapAction() {
        return "\"http://tempuri.org/" + operationName() + "\"";
    }

    private String operationName() {
        if (add != null) {
            return "Add";
        } else if (divide != null) {
            return "Divide";
        } else if (multiply != null) {
            return "Multiply";
        } else {
            return "Subtract";
        }
    }
}

