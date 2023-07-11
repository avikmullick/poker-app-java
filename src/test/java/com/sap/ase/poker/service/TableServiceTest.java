package com.sap.ase.poker.service;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Supplier;

class TableServiceTest {

  private TableService tableService;

  private Supplier<Deck> deckSupplier;

  private Deck deck;

  private Card card;

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
    tableService.addPlayer("01", "Chendil");
    tableService.addPlayer("02", "Smitha");
    Assertions.assertThat(tableService.getPlayers().get(0).isActive()).isFalse();
    Assertions.assertThat(tableService.getPlayers().get(1).isActive()).isFalse();
    Assertions.assertThat(tableService.getCurrentPlayer()).isNotPresent();
    Mockito.when(deckSupplier.get()).thenReturn(deck);
    Mockito.when(deck.draw()).thenReturn(card);
    tableService.start();
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
    String firstPlayerId = "01";
    String secondPlayerId = "02";
    tableService.addPlayer(firstPlayerId, "Chendil");
    tableService.addPlayer(secondPlayerId, "Smitha");
    Mockito.when(deckSupplier.get()).thenReturn(deck);
    Mockito.when(deck.draw()).thenReturn(card);
    //Before Start of the game
    Assertions.assertThat(tableService.getPlayerCards(firstPlayerId)).isEmpty();
    Assertions.assertThat(tableService.getPlayerCards(secondPlayerId)).isEmpty();

    tableService.start();
    //After Start of the game
    Assertions.assertThat(tableService.getPlayerCards(firstPlayerId)).isNotEmpty();
    Assertions.assertThat(tableService.getPlayerCards(firstPlayerId)).hasSize(2);
    Assertions.assertThat(tableService.getPlayerCards(secondPlayerId)).isNotEmpty();
    Assertions.assertThat(tableService.getPlayerCards(secondPlayerId)).hasSize(2);
  }

}