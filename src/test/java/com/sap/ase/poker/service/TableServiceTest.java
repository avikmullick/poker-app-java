package com.sap.ase.poker.service;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Deck;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.sap.ase.poker.model.deck.Card;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class TableServiceTest {

  private TableService tableService;
  private Supplier<Deck> deckSupplier;

  private Deck deck ;

  private Card card;

  @BeforeEach
  public void setup() {
    deckSupplier = Mockito.mock(Supplier.class) ;
    tableService = new TableService(deckSupplier);
    deck = Mockito.mock(Deck.class) ;
    card = Mockito.mock(Card.class) ;
  }

  @Test
  void getInitialStateAsOpen() {
    Assertions.assertThat(tableService.getState()).isEqualTo(GameState.OPEN) ;
  }

  @Test
  void checkGameStateInstance() {
    Assertions.assertThat(tableService.getState()).isInstanceOf(GameState.class) ;
  }
  // TODO: implement me

  @Test
  void getAllPlayers(){
    Assertions.assertThat(tableService.getPlayers()).isInstanceOf(List.class);
  }

  @Test
  void noPlayers(){
    Assertions.assertThat(tableService.getPlayers().size()).isEqualTo(0);
  }

  @Test
  void addPlayer(){
    tableService.addPlayer("01", "Chendil");
    Assertions.assertThat(tableService.getPlayers().get(0).getName()).isEqualTo("Chendil");
    Assertions.assertThat(tableService.getPlayers().get(0).getCash()).isEqualTo(100);
    Assertions.assertThat(tableService.getPlayers().get(0).isActive()).isEqualTo(false);
  }

  @Test
  void checkStartConditions(){
    tableService.addPlayer("01", "Chendil");
    tableService.addPlayer("02", "Smitha");
    Assertions.assertThat(tableService.getPlayers().get(0).isActive()).isEqualTo(false);
    Assertions.assertThat(tableService.getPlayers().get(1).isActive()).isEqualTo(false);
    Assertions.assertThat(tableService.getCurrentPlayer().isPresent()).isEqualTo(false);
    Mockito.when(deckSupplier.get()).thenReturn(deck);
    Mockito.when(deck.draw()).thenReturn(card);
    tableService.start();
    Assertions.assertThat(tableService.getState()).isEqualTo(GameState.PRE_FLOP);
    Assertions.assertThat(tableService.getPlayers().size()).isGreaterThanOrEqualTo(2);
    Assertions.assertThat(tableService.getPlayers().get(0).getHandCards().size()).isEqualTo(2);
    Assertions.assertThat(tableService.getPlayers().get(1).getHandCards().size()).isEqualTo(2);
    Assertions.assertThat(tableService.getCurrentPlayer().get()).isEqualTo(tableService.getPlayers().get(0));
    Assertions.assertThat(tableService.getPlayers().get(0).isActive()).isEqualTo(true);
    Assertions.assertThat(tableService.getPlayers().get(1).isActive()).isEqualTo(true);
  }

  @Test
  void startNotPossibleWhenPlayersAreNotSufficient(){
    tableService.addPlayer("01", "Chendil");
    tableService.start();
    Assertions.assertThat(tableService.getState()).isNotEqualTo(GameState.PRE_FLOP);
  }


}