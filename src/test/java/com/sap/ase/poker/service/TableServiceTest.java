package com.sap.ase.poker.service;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.function.Supplier;

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
  void performActionCheck() {
    setupForStartGame();
    //test case to perform Action
    Player currentPlayer= tableService.getCurrentPlayer().get();
    tableService.performAction("check",0);
    Assertions.assertThat(currentPlayer).isNotEqualTo(tableService.getCurrentPlayer().get());
  }

  @Test
  void performActionCheckIllegalAmount() {
    setupForStartGame();
    //test case to perform Action
    Player currentPlayer = tableService.getCurrentPlayer().get();
    IllegalAmountException illegalAmountException = org.junit.jupiter.api.Assertions.assertThrows(
      IllegalAmountException.class, () -> {
        tableService.performAction("check", 20);
      });
  }


  @Test
  void performActionCheckAllCheckedPlayers() {
    setupForStartGame();
    //test case to perform Action
    Player playerChendil = tableService.getCurrentPlayer().get();
    tableService.performAction("check",0);
    Assertions.assertThat(tableService.getCurrentPlayer().get().getId()).isEqualTo(secondPlayerId);
    Player playerSmitha = tableService.getCurrentPlayer().get();
    tableService.performAction("check",0);
    Assertions.assertThat(tableService.getState()).isEqualTo(GameState.FLOP);
    Assertions.assertThat(tableService.getCommunityCards()).hasSize(3);
    Assertions.assertThat(tableService.getCurrentPlayer().get().getId()).isEqualTo(firstPlayerId);
    //should return a list with one Card-class instance per community card
    if(tableService.getState().equals(GameState.FLOP)) {
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
    tableService.performAction("check",0);
    Assertions.assertThat(tableService.getCurrentPlayer()).isNotPresent();
  }

  @Test
  void performActionRaiseIllegalAmount() {
    setupForStartGame();
    //test case to perform Action
    Player currentPlayer = tableService.getCurrentPlayer().get();
    org.junit.jupiter.api.Assertions.assertThrows(
      IllegalAmountException.class, () -> {
        tableService.performAction("raise", 0);
      });

    tableService.performAction("raise", 40);
    org.junit.jupiter.api.Assertions.assertThrows(
      IllegalAmountException.class, () -> {
        tableService.performAction("raise", 30);
      });

  }

  @Test
  void performActionRaiseDeductCash() {
    setupForStartGame();
    //test case to perform Action
    Player currentPlayer = tableService.getCurrentPlayer().get();
    int previousPlayerCash = currentPlayer.getCash();
    tableService.performAction("raise", 40);
    int currentPlayerCash = currentPlayer.getCash();
    Assertions.assertThat(currentPlayerCash).isEqualTo(previousPlayerCash-40);
  }

  @Test
  void performActionRaiseIllegalExceptionDeductCash() {
    setupForStartGame();
    Player currentPlayer = tableService.getCurrentPlayer().get();
    //If the amount of the raise exceeds the player's remaining cash, an IllegalAmountException should be thrown
    org.junit.jupiter.api.Assertions.assertThrows(
      IllegalAmountException.class, () -> {
        tableService.performAction("raise", currentPlayer.getCash()+50);
      });
  }

  @Test
  void returnEmptyCommunityCardsInPreFlop(){
    setupForStartGame();
    if(tableService.getState().equals(GameState.PRE_FLOP)) {
      Assertions.assertThat(tableService.getCommunityCards()).isEmpty();
    }
  }
  
  private void setupForStartGame(){

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