package com.sap.ase.poker.service;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalActionException;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.InactivePlayerException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import com.sap.ase.poker.model.rules.Winners;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class TableService {

    private final Supplier<Deck> deckSupplier;
    private GameState gameState;

    private List<Player> playerList;

    private List<Card> communityCardList;

    private Player currentPlayer;
    private Player winnerPlayer;

    private int currentPlayerIndex;

    private int lastBetAmount;

    private int potAmount;

    private HashMap<String, Integer> playersBetMap;

    private Winners winners;

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
        return Optional.ofNullable(winnerPlayer);
    }

    public List<Card> getWinnerHand() {
        if(winnerPlayer!=null){
            return winnerPlayer.getHandCards();
        } else {
            return new ArrayList<Card>();
        }
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
        GameState oldGameState = this.gameState ;
        switch (action) {
            case "check":
                if (amount != 0) {
                    throw new IllegalAmountException("During check action, bet amount should be zero.");
                }
                if (lastBetAmount > 0){
                    throw new IllegalActionException("Check action invalid, as previous bet amount exists");
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
                int lastPlayerBet = 0;
                if ( currentPlayerIndex != 0 ) {
                    lastPlayerBet = playerList.get(currentPlayerIndex - 1).getBet();
                }else{
                    lastPlayerBet = playerList.get(playerList.size()- 1).getBet();
                }
                if (currentPlayer.getCash() < lastPlayerBet - currentPlayer.getBet()) {
                    throw new IllegalAmountException("The amount of call exceeds the player's remaining cash.");
                }

                currentPlayer.bet(lastPlayerBet - currentPlayer.getBet());
                playersBetMap.put(currentPlayer.getId(), currentPlayer.getBet());
                betAmount = lastBetAmount;
                break;
            case "fold":
                int count = 0;

                Player currentPlayer = getCurrentPlayer().get();
                currentPlayer.setInactive();
                Player expectedWinner=null;
                for (Player player : playerList
                ) {
                    if ((player != currentPlayer && player.isActive())) {
                        count++;
                        expectedWinner=player;
                        if (count > 1) {
                            break;
                        }
                    }
                }

                if (count == 1) {
                    winnerPlayer=expectedWinner;
                    this.gameState = GameState.ENDED;
                }
                break;
            default:
                throw new IllegalActionException("Action is Invalid " + action);
        }
        lastBetAmount = betAmount;
        System.out.printf("Action performed: %s, amount: %d%n", action, amount);
        deriveNextPlayerToBeCurrentPlayer();
        /** Check end of round. If round has ended, change game state and calculate pot amount
         * Change the game state now **/

        if ( currentPlayerIndex == 0 && oldGameState == this.gameState && checkEndOfRound()){
            changeGameState();
            calculatePotAmount();
        }
        if(gameState == GameState.ENDED){
            //Kailash ----DETERMINE_WINNERS, POT DISTRIBUTION
        }
    }

    private boolean checkEndOfRound() {
        int bet = 0;
            for (Player player : playerList) {
                if (player.isActive()) {
                    if (bet == 0 ) {
                        bet = player.getBet() ;
                    }else {
                        if( bet != player.getBet())
                        {
                           return false;
                        }
                    }
                }
            }
        return true;
    }

    private void changeGameState() {
        switch (gameState) {
            case PRE_FLOP:
                gameState = GameState.FLOP;
                break;
            case FLOP:
                gameState = GameState.TURN;
                break;
            case TURN:
                gameState = GameState.RIVER;
                break;
            case RIVER:
                gameState = GameState.ENDED;
                break;
        }
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

    private void calculatePotAmount() {
        for (Player player : playerList) {
            if (player.isActive()) {
                this.potAmount = this.potAmount + player.getBet();
            }
        }
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
