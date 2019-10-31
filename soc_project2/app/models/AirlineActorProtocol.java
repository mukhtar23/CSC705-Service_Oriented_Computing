package models;

public class AirlineActorProtocol {

  static public class SayHello {
    public final String name;

    public SayHello(String name) {
      this.name = name;
    }
  }
}