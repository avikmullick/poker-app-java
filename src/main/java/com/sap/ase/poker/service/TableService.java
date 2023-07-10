package com.sap.ase.poker.service;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import com.sap.ase.poker.model.deck.Kind;
import com.sap.ase.poker.model.deck.Suit;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

@Service
public class TableService {

    private final Supplier<Deck> deckSupplier;
    private GameState gameState ;

    public TableService(Supplier<Deck> deckSupplier) {
        this.deckSupplier = deckSupplier;
        this.gameState = GameState.OPEN;
    }

    public GameState getState() {
        return gameState;
    }

    public List<Player> getPlayers() {
        // TODO: implement me
        return Arrays.asList(new Player("al-capone", "Al Capone", 100),
                new Player("alice", "Alice", 100)
        );
    }

    public List<Card> getPlayerCards(String playerId) {
        // TODO: implement me
        return Arrays.asList(new Card(Kind.JACK, Suit.CLUBS),
                new Card(Kind.TEN, Suit.CLUBS)
        );
    }

    public List<Card> getCommunityCards() {
        // TODO: implement me
        return Arrays.asList(
                new Card(Kind.ACE, Suit.CLUBS),
                new Card(Kind.KING, Suit.CLUBS),
                new Card(Kind.QUEEN, Suit.CLUBS),
                new Card(Kind.FOUR, Suit.HEARTS),
                new Card(Kind.SEVEN, Suit.SPADES)
        );
    }

    public Optional<Player> getCurrentPlayer() {
        // TODO: implement me
        return Optional.of(new Player("al-capone", "Al", 100));
    }

    public Map<String, Integer> getBets() {
        // TODO: implement me
        return new HashMap<String, Integer>() {
            {
                put("al-capone", 100);
                put("alice", 50);
            }
        };
    }

    public int getPot() {
        // TODO: implement me
        return 150;
    }

    public Optional<Player> getWinner() {
        // TODO: implement me
        return Optional.of(new Player("al-capone", "Al capone", 500));
    }

    public List<Card> getWinnerHand() {
        // TODO: implement me
        return Arrays.asList(
                new Card(Kind.ACE, Suit.CLUBS),
                new Card(Kind.KING, Suit.CLUBS),
                new Card(Kind.QUEEN, Suit.CLUBS),
                new Card(Kind.JACK, Suit.CLUBS),
                new Card(Kind.TEN, Suit.CLUBS)
        );
    }

    public void start() {
        // TODO: implement me
    }

    public void addPlayer(String playerId, String playerName) {
        // TODO: implement me
        System.out.printf("Player joined the table: %s%n", playerId);
    }

    public void performAction(String action, int amount) throws IllegalAmountException {
        // TODO: implement me
        System.out.printf("Action performed: %s, amount: %d%n", action, amount);
    }

}
