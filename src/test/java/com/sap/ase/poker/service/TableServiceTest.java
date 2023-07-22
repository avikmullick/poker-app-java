package com.sap.ase.poker.service;

import com.sap.ase.poker.dto.GetTableResponseDto;
import com.sap.ase.poker.dto.PlayerDto;
import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalActionException;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.InactivePlayerException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import com.sap.ase.poker.model.rules.WinnerRules;
import com.sap.ase.poker.model.rules.Winners;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class TableServiceTest {

    private TableService tableService;

    private Supplier<Deck> deckSupplier;

    private Deck deck;

    private Card card;

    private String firstPlayerId;
    private String secondPlayerId;

    @BeforeEach
    public void setup() {
        deckSupplier = Mockito.mock(Supplier.class);
        tableService = new TableService(deckSupplier);
        deck = Mockito.mock(Deck.class);
        card = Mockito.mock(Card.class);

    }

    @Test
    void getInitialStateAsOpen() {
        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.OPEN);
    }

    @Test
    void checkGameStateInstance() {
        Assertions.assertThat(tableService.getState()).isInstanceOf(GameState.class);
    }
    // TODO: implement me

    @Test
    void getAllPlayers() {
        Assertions.assertThat(tableService.getPlayers()).isInstanceOf(List.class);
    }

    @Test
    void noPlayers() {
        Assertions.assertThat(tableService.getPlayers()).isEmpty();
    }

    @Test
    void addPlayer() {
        tableService.addPlayer("01", "Chendil");
        Assertions.assertThat(tableService.getPlayers().get(0).getName()).isEqualTo("Chendil");
        Assertions.assertThat(tableService.getPlayers().get(0).getCash()).isEqualTo(100);
        Assertions.assertThat(tableService.getPlayers().get(0).isActive()).isFalse();
    }

    @Test
    void duplicatePlayer() {
        tableService.addPlayer("01", "Chendil");
        tableService.addPlayer("01", "Chendil");
        Assertions.assertThat(tableService.getPlayers()).hasSize(1);
    }

    @Test
    void checkStartConditions() {

        setupForStartGame();
        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.PRE_FLOP);
        Assertions.assertThat(tableService.getPlayers()).hasSizeGreaterThanOrEqualTo(2);
        Assertions.assertThat(tableService.getPlayers().get(0).getHandCards()).hasSize(2);
        Assertions.assertThat(tableService.getPlayers().get(1).getHandCards()).hasSize(2);
        Assertions.assertThat(tableService.getCurrentPlayer()).contains(tableService.getPlayers().get(0));
        Assertions.assertThat(tableService.getPlayers().get(0).isActive()).isTrue();
        Assertions.assertThat(tableService.getPlayers().get(1).isActive()).isTrue();
    }

    @Test
    void startNotPossibleWhenPlayersAreNotSufficient() {
        tableService.addPlayer("01", "Chendil");
        tableService.start();
        Assertions.assertThat(tableService.getState()).isNotEqualTo(GameState.PRE_FLOP);
    }

    @Test
    void getCurrentPlayer() {
        if (tableService.getState().equals(GameState.OPEN)) {
            Assertions.assertThat(tableService.getCurrentPlayer()).isNotPresent();
        } else {
            Assertions.assertThat(tableService.getCurrentPlayer()).isPresent();
        }
    }

    /**
     * This test case is when unknown user trying to get into game and fetching getPlayerCards
     */
    @Test
    void getPlayerCardsWhenEmpty() {
        Assertions.assertThat(tableService.getPlayerCards("Kailash")).isEmpty();
    }

    /**
     * This test case is when user joined the game and trying to fetch getPlayerCards
     * Covered - should return an empty list if no cards have been dealt yet
     */
    @Test
    void getPlayerCardsWhenNotEmpty() {

        setupForStartGame();
        //After Start of the game
        Assertions.assertThat(tableService.getPlayerCards(firstPlayerId)).isNotEmpty();
        Assertions.assertThat(tableService.getPlayerCards(firstPlayerId)).hasSize(2);
        Assertions.assertThat(tableService.getPlayerCards(secondPlayerId)).isNotEmpty();
        Assertions.assertThat(tableService.getPlayerCards(secondPlayerId)).hasSize(2);
    }

    @Test
    void returnEmptyCommunityCardsInPreFlop() {
        setupForStartGame();
        if (tableService.getState().equals(GameState.PRE_FLOP)) {
            Assertions.assertThat(tableService.getCommunityCards()).isEmpty();
        }
    }

    @Test
    void performActionCheck() {
        setupForStartGame();
        //test case to perform Action
        Player currentPlayer = tableService.getCurrentPlayer().get();
        tableService.performAction("check", 0);
        Assertions.assertThat(currentPlayer).isNotEqualTo(tableService.getCurrentPlayer().get());
    }

    @Test
    void performActionCheckWithBetAmountNotZero(){
        setupForStartGame();
        tableService.performAction("check", 0);
        tableService.performAction("check", 0);
        tableService.performAction("raise", 10);

        Assertions.assertThatThrownBy(() -> {
            tableService.performAction("check", 0);
        }).isInstanceOf(IllegalActionException.class).hasMessage("Check action invalid, as previous bet amount exists");
    }


    @Test
    void checkEndOfRound(){
        setupForStartGame();
        tableService.performAction("check", 0);
        tableService.performAction("check", 0);
        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.FLOP);
        Assertions.assertThat(tableService.getPot()).isEqualTo(0);
        tableService.performAction("raise", 20);
        tableService.performAction("call",0);
 //Bets are equal, End of round
        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.TURN);
        tableService.performAction("raise", 50);
        tableService.performAction("call", 0);
//Bets are equal, end of round , change of state
        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.RIVER);
        Assertions.assertThat(tableService.getPot()).isEqualTo(180);
    }

    @Test
    void raiseEndOfRound(){
        setupForStartGame();
        tableService.performAction("check", 0);
        tableService.performAction("check", 0);
        tableService.performAction("raise", 10);
        tableService.performAction("raise", 20);
        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.FLOP);
    }

    @Test
    void performActionCheckIllegalAmount() {
        setupForStartGame();
        //test case to perform Action
        Player currentPlayer = tableService.getCurrentPlayer().get();
        Assertions.assertThatThrownBy(() -> {
            tableService.performAction("check", 20);
        }).isInstanceOf(IllegalAmountException.class).hasMessage("During check action, bet amount should be zero.");
    }

    @Test
    void performActionCheckAllCheckedPlayers() {
        setupForStartGame();
        //test case to perform Action
        Player playerChendil = tableService.getCurrentPlayer().get();
        tableService.performAction("check", 0);
        Assertions.assertThat(tableService.getCurrentPlayer().get().getId()).isEqualTo(secondPlayerId);
        Player playerSmitha = tableService.getCurrentPlayer().get();
        tableService.performAction("check", 0);
        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.FLOP);
        Assertions.assertThat(tableService.getCommunityCards()).hasSize(3);
        Assertions.assertThat(tableService.getCurrentPlayer().get().getId()).isEqualTo(firstPlayerId);
        //should return a list with one Card-class instance per community card
        if (tableService.getState().equals(GameState.FLOP)) {
            Assertions.assertThat(tableService.getCommunityCards()).isNotEmpty();
            Assertions.assertThat(tableService.getCommunityCards()).isInstanceOf(List.class);
        }
    }

    /**
     * inactive players will not be considered for the next player's determination
     */
    @Test
    void performActionCheckForInactiveUser() {
        //will not start the game and performAction
        firstPlayerId = "01";
        secondPlayerId = "02";
        tableService.addPlayer(firstPlayerId, "Chendil");
        tableService.addPlayer(secondPlayerId, "Smitha");
        Assertions.assertThatThrownBy(() -> {
            tableService.performAction("check", 0);
        }).isInstanceOf(InactivePlayerException.class).hasMessage("All players cannot be inactive");
        Assertions.assertThat(tableService.getCurrentPlayer()).isNotPresent();
    }

    @Test
    void performActionRaiseIllegalAmount() {
        setupForStartGame();
        //test case to perform Action
        //Raise amount must be strictly higher than the current bet. Current bet cannot be zero.
        Player currentPlayer = tableService.getCurrentPlayer().get();
        Assertions.assertThatThrownBy(() -> {
            tableService.performAction("raise", 0);
        }).isInstanceOf(IllegalAmountException.class).hasMessage("Raise amount must be strictly higher than the current bet. Current bet cannot be zero.");

        tableService.performAction("raise", 40);
        Assertions.assertThatThrownBy(() -> {
            tableService.performAction("raise", 30);
        }).isInstanceOf(IllegalAmountException.class).hasMessage("Raise amount must be strictly higher than the current bet. Current bet cannot be zero.");
    }

    @Test
    void performActionRaiseDeductCash() {
        setupForStartGame();
        //test case to perform Action
        Player currentPlayer = tableService.getCurrentPlayer().get();
        int previousPlayerCash = currentPlayer.getCash();
        tableService.performAction("raise", 40);
        int currentPlayerCash = currentPlayer.getCash();
        Assertions.assertThat(currentPlayerCash).isEqualTo(previousPlayerCash - 40);
    }

    @Test
    void performActionRaiseIllegalExceptionDeductCash() {
        setupForStartGame();
        Player currentPlayer = tableService.getCurrentPlayer().get();
        int raisedAmount = currentPlayer.getCash() + 50;
        //If the amount of the raise exceeds the player's remaining cash, an IllegalAmountException should be thrown
        Assertions.assertThatThrownBy(() -> {
            tableService.performAction("raise", raisedAmount);
        }).isInstanceOf(IllegalAmountException.class).hasMessage("The amount of the raise exceeds the player's remaining cash.");
    }

    @Test
    void performActionRaiseIllegalExceptionExceedsOtherPlayersCash() {
        setupForStartGame();
        Player currentPlayer = tableService.getCurrentPlayer().get();
        tableService.performAction("raise", 60);
        //If the amount of the raise exceeds any other players remaining cash an IllegalAmountException should be thrown, e.g. if Bob only has 10 left, Alice cannot raise to more than 10
        Assertions.assertThatThrownBy(() -> {
            tableService.performAction("raise", 90);
        }).isInstanceOf(IllegalAmountException.class).hasMessageContaining("The amount of the raise exceeds any other players remaining cash.");
    }

    @Test
    void performActionRaiseNextPlayerRaisingMoreAmount() {
        setupForStartGame();
        //Raising can also be done by a player, if the previous player raised already, e.g. if Bob raised before to 10, Alice can raise to 20
        Player currentPlayer = tableService.getCurrentPlayer().get();
        tableService.performAction("raise", 40);
        tableService.performAction("raise", 50);
        //Game continues
        Assertions.assertThat(tableService.getCurrentPlayer().get().getId()).isEqualTo(firstPlayerId);
        Assertions.assertThat(tableService.getBets()).containsEntry(currentPlayer.getId(), currentPlayer.getBet());
        Assertions.assertThat(tableService.getBets()).hasSize(2);
    }

    @Test
    void performInvalidAction() {
        setupForStartGame();
        //Invalid Action
        Player currentPlayer = tableService.getCurrentPlayer().get();
        Assertions.assertThatThrownBy(() -> {
            tableService.performAction("invalid", 40);
        }).isInstanceOf(IllegalActionException.class);
    }

    @Test
    void performCallWithoutRaise() {

        Assertions.assertThatThrownBy(() -> {
            tableService.performAction("call", 40);
        }).isInstanceOf(IllegalActionException.class).hasMessage("Call not possible before Raise");
    }

    @Test
    void performCallAfterRaise() {

        setupForStartGame();
        tableService.performAction("raise", 49);
        Player oldPlayer = tableService.getCurrentPlayer().get();
        int oldPlayerCash = oldPlayer.getCash();
        tableService.performAction("call", 0);
        //Check if cash is deducted
        Assertions.assertThat(oldPlayer.getCash()).isEqualTo(oldPlayerCash - 49);
        //Check if current player is the next player in the list
        Player newPlayer = tableService.getCurrentPlayer().get();
        Assertions.assertThat(newPlayer).isNotSameAs(oldPlayer);

    }

    @Test
    void performRaiseRaiseCall() {
        setupForStartGame();
        Player firstPlayer = tableService.getCurrentPlayer().get();
        int firstPlayerCash = firstPlayer.getCash();
        //first player raise 40
        tableService.performAction("raise", 40);
        //second player raise 50
        tableService.performAction("raise", 50);
        //first player calls -> his total cash reduces by 50
        tableService.performAction("call", 0);
        //Check if cash is deducted
        Assertions.assertThat(firstPlayer.getCash()).isEqualTo(firstPlayerCash - 50);
    }

    @Test
    void foldMarkPlayerAsInactive() {

        setupForStartGame();

        Player currentPlayer = tableService.getCurrentPlayer().get();
        tableService.performAction("fold", 0);

        Assertions.assertThat(currentPlayer.isActive()).isFalse();

    }

    @Test
    void foldEndGame() {

        setupForStartGame();
        Player currentPlayer = tableService.getCurrentPlayer().get();
        tableService.performAction("fold", 0);
        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.ENDED);
    }

    @Test
    void foldFoldEndGame() {

        tableService.addPlayer("03", "Avik");
        setupForStartGame();
        Player currentPlayer = tableService.getCurrentPlayer().get();

        //first player fold
        tableService.performAction("fold", 0);

        //Second player fold
        tableService.performAction("fold", 0);

        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.ENDED);
    }

    @Test
    void getWinnerTest(){

        tableService.addPlayer("03", "Avik");
        setupForStartGame();
        Player currentPlayer = tableService.getCurrentPlayer().get();

        //first player fold
        tableService.performAction("fold", 0);

        //Second player fold
        tableService.performAction("fold", 0);

        Assertions.assertThat(tableService.getState()).isEqualTo(GameState.ENDED);
// On state ENDED - Implementation of determine winners is pending.
        Assertions.assertThat(tableService.getWinner().isPresent()).isTrue();

    }

    @Test
    @DisplayName("Showdown ended in a draw, return the first winner")
    void getFirstWinnerShowdownIsDrawTest(){

        tableService.addPlayer("03", "Avik");
        setupForStartGame();
        Player currentPlayer = tableService.getCurrentPlayer().get();

        //first player fold
        tableService.performAction("fold", 0);

        //Second player fold
        tableService.performAction("fold", 0);

        assertThat(tableService.getState()).isEqualTo(GameState.ENDED);
// On state ENDED - Implementation of determine winners is pending.
         Assertions.assertThat(tableService.getWinners().get(0).getName()).isEqualTo("Avik");

    }

    @Test
    @DisplayName("Return empty winnerHand if all players folded")
    void getEmptyWinnerHandList(){
        tableService.addPlayer("03", "Avik");
        setupForStartGame();
        Player currentPlayer = tableService.getCurrentPlayer().get();

        //first player fold
        tableService.performAction("fold", 0);

        //Second player fold
        tableService.performAction("fold", 0);

        //Third player fold
       // tableService.performAction("fold", 0);

        assertThat(tableService.getState()).isEqualTo(GameState.ENDED);
// On state ENDED - Implementation of determine winners and winning hand is pending.
        Assertions.assertThat(tableService.getWinnerHand()).isEmpty();

    }

    @Test
    @DisplayName("Get best hand of the winner")
    void getWinnerBestHand(){
        tableService.addPlayer("03", "Avik");
        setupForStartGame();
        Player currentPlayer = tableService.getCurrentPlayer().get();

        //first player fold
        tableService.performAction("fold", 0);

        //Second player fold
        tableService.performAction("fold", 0);

        assertThat(tableService.getState()).isEqualTo(GameState.ENDED);
// On state ENDED - Implementation of determine winners and winning hand is pending.
        Assertions.assertThat(tableService.getWinnerHand()).isNotEmpty();

    }


    @Test
    @DisplayName("Showdown ended a DRAW, return hand of first winner")
    void getFirstWinnerBestHand(){
        tableService.addPlayer("03", "Avik");
        setupForStartGame();
        Player currentPlayer = tableService.getCurrentPlayer().get();

        //first player fold
        tableService.performAction("fold", 0);

        //Second player fold
        tableService.performAction("fold", 0);

        //Third player fold
        // tableService.performAction("fold", 0);

        assertThat(tableService.getState()).isEqualTo(GameState.ENDED);
// On state ENDED - Implementation of determine winners and winning hand is pending.
        Assertions.assertThat(tableService.getWinnerHand()).isNotEmpty();

    }
    @Test
    void gameStateChangeFromFlopToTurn(){
        setupForStartGame();
    }


    private void setupForStartGame() {

        firstPlayerId = "01";
        secondPlayerId = "02";

        tableService.addPlayer(firstPlayerId, "Chendil");
        tableService.addPlayer(secondPlayerId, "Smitha");

        //Before Start of the game
        Assertions.assertThat(tableService.getPlayerCards(firstPlayerId)).isEmpty();
        Assertions.assertThat(tableService.getPlayerCards(secondPlayerId)).isEmpty();
        Assertions.assertThat(tableService.getPlayers().get(0).isActive()).isFalse();
        Assertions.assertThat(tableService.getPlayers().get(1).isActive()).isFalse();
        Assertions.assertThat(tableService.getCurrentPlayer()).isNotPresent();
        Mockito.when(deckSupplier.get()).thenReturn(deck);
        Mockito.when(deck.draw()).thenReturn(card);
        tableService.start();

    }
}