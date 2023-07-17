package com.sap.ase.poker.service;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalActionException;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.InactivePlayerException;
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
    private GameState gameState;

    private List<Player> playerList;

    private List<Card> communityCardList;

    private Player currentPlayer;

    private int currentPlayerIndex;

    private int lastBetAmount;

    private int potAmount;

    private HashMap<String, Integer> playersBetMap;

    public TableService(Supplier<Deck> deckSupplier) {
        this.deckSupplier = deckSupplier;
        this.gameState = GameState.OPEN;
        this.playerList = new ArrayList<>();
        this.communityCardList = new ArrayList<>();
        currentPlayerIndex = 0;
        lastBetAmount = 0;
        playersBetMap = new HashMap<>();
        potAmount = 0;
    }

    public GameState getState() {
        return gameState;
    }

    public List<Player> getPlayers() {
        return playerList;
    }

    public List<Card> getPlayerCards(String playerId) {
        Player findPlayerUsingId = getFindPlayerUsingId(playerId);
        if (findPlayerUsingId != null) {
            return findPlayerUsingId.getHandCards();
        }
        return Collections.emptyList();
    }

    public List<Card> getCommunityCards() {
        return communityCardList;
    }

    public Optional<Player> getCurrentPlayer() {
        return Optional.ofNullable(currentPlayer);
    }

    public Map<String, Integer> getBets() {
        return playersBetMap;
    }

    public int getPot() {
        return potAmount;
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
        if (playerList.size() >= 2) {
            this.gameState = GameState.PRE_FLOP;
            for (Player player : playerList) {
                Card firstCard = deckSupplier.get().draw();
                Card secondCard = deckSupplier.get().draw();
                player.setHandCards(Arrays.asList(firstCard, secondCard));
                player.setActive();
            }
            this.currentPlayer = playerList.get(0);
            this.currentPlayerIndex = 0;
        }
    }

    public void addPlayer(String playerId, String playerName) {
        Player findPlayerUsingId = getFindPlayerUsingId(playerId);
        if (findPlayerUsingId != null) {
            System.out.printf("Duplicate Player %s%n", playerId);
            return;
        }
        Player newPlayer = new Player(playerId, playerName, 100);
        newPlayer.setInactive();
        playerList.add(newPlayer);
    }

    public void performAction(String action, int amount) throws IllegalAmountException, IllegalActionException {
        boolean validAction = false;
        int betAmount = amount;
        switch (action) {
            case "check":
                if (amount != 0) {
                    throw new IllegalAmountException("During check action, bet amount should be zero.");
                }
                //means allPlayers have checked
                if (currentPlayerIndex == playerList.size() - 1) {
                    this.gameState = GameState.FLOP;
                    communityCardList.add(deckSupplier.get().draw());
                    communityCardList.add(deckSupplier.get().draw());
                    communityCardList.add(deckSupplier.get().draw());
                }
                break;
            case "raise":
                if (amount <= lastBetAmount) {
                    throw new IllegalAmountException("Raise amount must be strictly higher than the current bet. Current bet cannot be zero.");
                }
                if (currentPlayer.getCash() < amount) {
                    throw new IllegalAmountException("The amount of the raise exceeds the player's remaining cash.");
                }
                otherPlayersRemainingCashCannotBeGreaterThanRaisedCash(amount);
                currentPlayer.bet(amount);
                playersBetMap.put(currentPlayer.getId(), currentPlayer.getBet());
                break;
            case "call":
                //Check if raise action has been called before call. If any other player other than current player has placed a bet, it can
                // be assumed that raise call has happened already
                for (Player player : playerList
                ) {
                    if ((player != currentPlayer && player.getBet() != 0)) {
                        validAction = true;
                        break;
                    }
                }
                if (!validAction) {
                    throw new IllegalActionException("Call not possible before Raise");
                }
                if (currentPlayer.getCash() < lastBetAmount - currentPlayer.getBet()) {
                    throw new IllegalAmountException("The amount of call exceeds the player's remaining cash.");
                }
                currentPlayer.bet(lastBetAmount - currentPlayer.getBet());
                playersBetMap.put(currentPlayer.getId(), currentPlayer.getBet());
                betAmount = lastBetAmount;
                break;
            default:
                throw new IllegalActionException("Action is Invalid " + action);

            case "fold":
                int count = 0;

                Player currentPlayer = getCurrentPlayer().get();
                currentPlayer.setInactive();

                for (Player player : playerList
                ) {
                    if ((player != currentPlayer && player.isActive())) {
                        count++;
                        if (count > 1) {
                            break;
                        }
                    }
                }

                if (count == 1) {
                    this.gameState = GameState.ENDED;
                }
        }
        deriveNextPlayerToBeCurrentPlayer();
        lastBetAmount = betAmount;
        System.out.printf("Action performed: %s, amount: %d%n", action, amount);
    }

    /**
     * Derive Next player
     */
    private void deriveNextPlayerToBeCurrentPlayer() {
        do {
            currentPlayerIndex++;
            if (playerList.size() == currentPlayerIndex) {
                currentPlayerIndex = 0;
                if (!playerList.get(currentPlayerIndex).isActive()) {
                    throw new InactivePlayerException("All players cannot be inactive");
                }
            }
        } while (!playerList.get(currentPlayerIndex).isActive());
        this.currentPlayer = playerList.get(currentPlayerIndex);
    }

    /**
     * If the amount of the raise exceeds any other players remaining cash an IllegalAmountException should be thrown,
     * e.g. if Bob only has 10 left, Alice cannot raise to more than 10
     *
     * @param raisedAmount
     */
    private void otherPlayersRemainingCashCannotBeGreaterThanRaisedCash(int raisedAmount) {
        for (Player player : playerList) {
            if (player.getCash() < raisedAmount) {
                throw new IllegalAmountException(
                        "The amount of the raise exceeds any other players remaining cash." + player.getName() + " only has "
                                + player.getCash() + " left, " + getCurrentPlayer() + " cannot raise to more than "
                                + player.getCash());
            }
        }
    }

    /**
     * Find player using playerId
     *
     * @param playerId
     * @return
     */
    private Player getFindPlayerUsingId(String playerId) {
        return playerList.stream()
                .filter(player -> playerId.equals(player.getId()))
                .findAny()
                .orElse(null);
    }
}
