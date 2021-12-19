# Net Rummy

This project is a class project for CS 4431 - Computer Networks I.

This project is an implementation of Rummy 500 over the internet. It utilizes a peer-to-peer connection to allow a player to set up their own lobby that others can join. If a player is hosting a lobby, they are unable to join another lobby at any point. This project only contains a command line interface.

## Running the Program

This program requires Java to be installed on the machine. Installation instructions can be found at https://java.com/en/download/help/download_options.html.

Once Java has been installed, a `git clone` should be completed followed by changing directory into the base folder of the repository (the same folder as this README). From this directory, the program can be compiled with:

```
javac src/Main.java
```

The program can then be run by doing:

```
java src/Main
```

Initial options for the program include 1) Joining another lobby, 2) Hosting a new lobby, or 3) Exiting. These can be specified by entering in `1`, `2`, or `3`. Additional prompts will be given to get connection information or to allow the game to begin. During the game, actions can be specified with:
- `D` to discard a card and end a turn
- `T` to take a card from the deck
- `P` to pick a card from the discard pile
- `S` to make a set