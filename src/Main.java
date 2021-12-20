package src;

import java.util.Scanner;
import java.net.InetAddress;

/**
 * Author: Samuel Fritz
 * CSCI 4431
 * 
 * The runner program for the game
 */
public class Main {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("Welcome to Networked Rummy\n");
        System.out.println("The rules are simple:");
        System.out.println("\tBe the first to run out of cards in your hand by putting down sets");
        System.out.println("\tThese sets can be made by making runs/straights of the same suit or by making sets of the same rank");
        System.out.println("\tAll sets must be at least three cards but can be added to later");
        System.out.println("\tPoints are given based on the value of the cards put down in sets");
        System.out.println("\tPoints are lost based on the value of cards still in your hand when the round ends");
        System.out.println("\tFace cards and aces are worth 10 points each. Non-face cards are worth their printed value");
        System.out.println("\tThe first player to reach 500 points wins\n\n");

        while (true) {
            System.out.println("What would you like to do?");
            System.out.println("1) Join a lobby");
            System.out.println("2) Host a game");
            System.out.println("3) Exit");
            
            String gameChoice = input.nextLine();

            if (gameChoice.equals("1")) {
                // Joining another player lobby - get ip and connect
                System.out.print("Enter host IP to connect to: ");
                String host = input.nextLine();
                while (host == null || host.equals("")) {
                    System.out.print("Invalid input - try again: ");
                    host = input.nextLine();
                }

                System.out.print("Enter name: ");
                String alias = input.nextLine();

                while (alias == null || alias.equals("")) {
                    System.out.print("Invalid input - try again: ");
                    alias = input.nextLine();
                }

                System.out.println("Connecting to host...");
                Player player = new Player(host, alias, false);

                // Let the game run until the host decides to end
                player.run();
            } else if (gameChoice.equals("2")) {
                // Start the host in the background
                System.out.println("Starting host...");
                Thread host = new Thread(new Host());
                host.start();

                // Then connect from the client-facing player program
                System.out.print("Enter name: ");
                String alias = input.nextLine();

                while (alias == null || alias.equals("")) {
                    System.out.print("Invalid input - try again: ");
                    alias = input.nextLine();
                }

                System.out.println("Connecting to host...");
                String address = null;
                try {
                    address = InetAddress.getLocalHost().getHostAddress();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Thread play = new Thread(new Player(address, alias, true));
                play.start();

                try {
                    play.join();
                    host.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (gameChoice.equals("3")) {
                break;
            } else {
                System.out.print("Invalid choice - try again: ");
            }
        }

        input.close();
    }
}
