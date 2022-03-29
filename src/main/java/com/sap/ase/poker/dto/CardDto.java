package com.sap.ase.poker.dto;

import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Kind;
import com.sap.ase.poker.model.deck.Suit;

public class CardDto {

    private String suit;
    private String kind;

    public CardDto() {
    }

    public CardDto(Card card) {
        suit = suitToString(card.suit);
        kind = kindToString(card.kind);
    }

    private String suitToString(Suit suit) {
        return suit.toString().toLowerCase();
    }

    private String kindToString(Kind kind) {
        switch (kind) {
            case ACE:
                return "ace";
            case TWO:
                return "2";
            case THREE:
                return "3";
            case FOUR:
                return "4";
            case FIVE:
                return "5";
            case SIX:
                return "6";
            case SEVEN:
                return "7";
            case EIGHT:
                return "8";
            case NINE:
                return "9";
            case TEN:
                return "10";
            case JACK:
                return "jack";
            case QUEEN:
                return "queen";
            case KING:
                return "king";
            default:
                throw new UnknownKindException(kind);
        }
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @SuppressWarnings("serial")
    private class UnknownKindException extends RuntimeException {
        public UnknownKindException(Kind kind) {
            super("unknown kind: " + kind);
        }
    }
}
