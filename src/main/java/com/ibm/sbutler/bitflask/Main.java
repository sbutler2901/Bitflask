package com.ibm.sbutler.bitflask;

import java.io.IOException;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) {
    System.out.println("Hello, world!");

    try {
      App app = new App();

      while(true) {
        Scanner input = new Scanner(System.in);

        String line = input.nextLine();
        String[] parsedInput = line.split("\\s+");

        String command = parsedInput[0];

        if (command.startsWith("!exit")) {
          break;
        } else if (command.startsWith("get") && parsedInput.length > 1) {
          String key = parsedInput[1];

          app.get(key);
        } else if (command.startsWith("set") && parsedInput.length > 2) {
          String key = parsedInput[1];
          String value = parsedInput[2];

          app.set(key, value);
        } else {
          System.out.println("Invalid input!");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
