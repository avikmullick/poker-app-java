package com.sap.ase.poker.acceptance;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sap.ase.poker.Exceptions.IllegalOperationException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.Table;

public class AcceptanceTest {
	private Table table;
	private static final String ALICE = "alice";
	private static final String BOB = "bob";
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setup() {
		table = new Table();
		table.addPlayer(ALICE);
		table.addPlayer(BOB);
		table.startGame();
	}

	@Test
	public void gameNotStarted_shouldGetBasicInfo() throws Exception {
		table = new Table();
		Player currentPlayer = table.getCurrentPlayer();
		currentPlayer.getBet();
		currentPlayer.getCards();
		currentPlayer.getCash();
		currentPlayer.getName();
		table.getCommunityCards();
		table.getPlayers();
	}

	@Test
	public void startGame() throws Exception {

		assertThat(table.getCurrentPlayer().getName(), is(ALICE));

		assertBetAndCash(ALICE, 1, 99);
		assertBetAndCash(BOB, 2, 98);

		assertThat(table.getCommunityCards().size(), is(0));

		table.call();
		assertThat(table.getCommunityCards().size(), is(0));
		assertThat(table.getCurrentPlayer().getName(), is(BOB));

		table.check();
		assertThat(table.getCommunityCards().size(), is(3));
		assertThat(table.getCurrentPlayer().getName(), is(ALICE));
	}

	@Test
	public void testFold() throws Exception {
		table.fold();
		assertThat(table.getCurrentPlayer().getCash(), is(101));
	}

	@Test
	public void testRaise() throws Exception {
		table.raiseTo(3);
		table.call();
		assertBetAndCash(ALICE, 3, 97);
		assertBetAndCash(BOB, 3, 97);
	}

	@Test
	public void checkThroughTillEnd() throws Exception {
		table.call();
		table.check();
		assertThat(table.getCommunityCards().size(), is(3));
		table.check();
		table.check();
		assertThat(table.getCommunityCards().size(), is(4));
		table.check();
		table.check();
		assertThat(table.getCommunityCards().size(), is(5));
		table.check();
		table.check();

		assertThat(table.getCurrentPlayer().getName(), is(BOB));
		assertThat(findPlayer(BOB).getBet(), is(1));
		assertThat(findPlayer(ALICE).getBet(), is(2));
	}

	@Test
	public void raiseAndCallTillEnd() throws Exception {
		table.raiseTo(3);
		table.call();
		assertThat(table.getCommunityCards().size(), is(3));
		table.raiseTo(4);
		table.call();
		assertThat(table.getCommunityCards().size(), is(4));
		table.raiseTo(5);
		table.call();
		assertThat(table.getCommunityCards().size(), is(5));
		table.raiseTo(6);
		table.call();

		assertThat(table.getCurrentPlayer().getName(), is(BOB));
		assertThat(findPlayer(BOB).getBet(), is(1));
		assertThat(findPlayer(ALICE).getBet(), is(2));
	}

	@Test(expected = IllegalOperationException.class)
	public void illegalCheck() throws Exception {
		table.check();
	}

	@Test(expected = IllegalOperationException.class)
	public void illegalRaiseExceedsCash() throws Exception {
		table.raiseTo(101);
	}

	@Test
	public void legalRaiseMaximum() throws Exception {
		table.raiseTo(100);
	}
	@Test(expected = IllegalOperationException.class)
	public void illegalRaiseLessOrEqualMaxBet() throws Exception {
		table.raiseTo(2);
	}
	
	@Test
	public void legalRaiseMinimum() throws Exception {
		table.raiseTo(3);
	}

	@Test
	public void illegalCall() throws Exception {
		table.call();
		thrown.expect(IllegalOperationException.class);
		table.call();
	}
	
	private void assertBetAndCash(String playerName, int bet, int cash) {
		Player player = findPlayer(playerName);
		assertThat(player.getBet(), is(bet));
		assertThat(player.getCash(), is(cash));
	}

	private Player findPlayer(String playerName) {
		for (Player p : table.getPlayers()) {
			if (p.getName().equals(playerName)) {
				return p;
			}
		}
		throw new RuntimeException("player " + playerName + " does not exist");
	}

}
