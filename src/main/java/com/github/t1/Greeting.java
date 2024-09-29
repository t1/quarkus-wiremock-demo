package com.github.t1;

public record Greeting(String address, String name) {
    @Override public String toString() {return address + ", " + name + "!";}
}
