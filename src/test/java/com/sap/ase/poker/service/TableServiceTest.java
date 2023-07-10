package com.sap.ase.poker.service;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.deck.Deck;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class TableServiceTest {

  private TableService tableService;
  private Supplier<Deck> deckSupplier;

  @BeforeEach
  public void setup() {
    deckSupplier = Mockito.mock(Supplier.class) ;
    tableService = new TableService(deckSupplier);
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
}