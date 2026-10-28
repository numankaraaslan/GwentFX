package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.SpecialType;

/**
 * The texts of the "How to play" screen. Text between two {@code **} is shown emphasised.
 * Ability, special card and faction perk descriptions come from the model enums; this class adds the explanations
 * and tips around them.
 */
final class RulesText {

    // ------------------------------------------------------------------ goal

    static final String NUTSHELL_TITLE = "In a nutshell";
    static final String NUTSHELL_1 = "Win **two out of three rounds**. Each player has **two gems** (lives) and loses one for every round lost.";
    static final String NUTSHELL_2 = "On your turn, **play one card** from your hand or **use your leader** — or **pass** and sit out the rest of the round.";
    static final String NUTSHELL_3 = "When both players have passed, the side with the higher **total strength** on the battlefield wins the round.";
    static final String NUTSHELL_4 = "Nobody draws a card at the start of a turn: the hand you are dealt has to last the **whole game**.";
    static final String GOAL_1 = "Gwent is a battle between two armies, fought with cards. A game lasts **up to three rounds**, and whoever wins **two rounds** wins the game.";
    static final String GOAL_2 = "Each player starts with **two gems** — your lives. Every round you lose costs you a gem; when your last gem is gone, you have lost the game. If both players lose their last gem in the same round, the game ends in a **draw**.";
    static final String GEMS_FULL = "Two gems: you haven't lost a round yet.";
    static final String GEMS_ONE = "One gem left: lose one more round and the game is over.";
    static final String GOAL_3 = "You win a round by having **more strength on your side of the battlefield** than your opponent once both of you have passed. Every unit card has a strength number, and the board adds them up for you.";

    // ------------------------------------------------------------------ a round

    static final String ROUND_1 = "Players take turns. On your turn you do **exactly one** of these things:";
    static final String ROUND_PLAY = "**Play one card** from your hand: a unit, a special card or a weather card.";
    static final String ROUND_LEADER = "**Use your leader's ability** (once per game).";
    static final String ROUND_PASS = "**Pass**: you are finished for this round and can't play any more cards until the next round starts.";
    static final String ROUND_2 = "After you pass, your opponent may go on playing, one card per turn, for as long as they like, and pass whenever they are ready. A player with no cards left (and no usable leader ability) passes automatically.";
    static final String ROUND_3 = "The round ends as soon as **both players have passed**. Whoever has the higher total strength wins the round; the loser loses a gem. In a **draw, both players lose a gem** — unless exactly one of them plays **Nilfgaard**, whose perk wins every drawn round.";
    static final String ROUND_4 = "Then the battlefield is cleared: every card on it goes to its owner's **discard pile** (the graveyard), and the next round starts on an empty board. The cards still in your hand **stay in your hand**, but you get no new ones — only spies, the Northern Realms perk and a few leaders give you extra cards.";
    static final String ROUND_5 = "The **winner of a round starts the next round**. After a drawn round, the player who made the last move starts.";

    // ------------------------------------------------------------------ card advantage

    static final String ADVANTAGE_1 = "Because nobody draws a card each turn, **the cards in your hand are your most precious resource**. Every card you play is gone for the rest of the game, and the player with **more cards left for the last round** usually wins.";
    static final String ADVANTAGE_2 = "So never try to win a round at any price. **Passing at the right moment** is the most important skill in Gwent: when you are ahead, pass and make your opponent spend cards to catch up. When you are far behind, pass as well — losing a round but keeping your cards is often better than winning it with your whole hand.";
    static final String ADVANTAGE_3 = "A good rule of thumb: **win a round while playing fewer cards than your opponent**. Losing the first round is fine if you start the second one with more cards.";
    static final String ADVANTAGE_4 = "These cards are worth more than one card each — that is called **card advantage**:";
    static final String ADVANTAGE_SPY = "Played on your opponent's side, and you **draw 2 cards**: you spend one card and get two.";
    static final String ADVANTAGE_MEDIC = "Brings a unit back from your discard pile: **two units for one card**.";
    static final String ADVANTAGE_MUSTER = "One card pulls **all its companions** from your deck and your hand onto the battlefield.";
    static final String ADVANTAGE_DECOY = "Takes a unit on your side back into your hand to **play it again** — a Medic, or a spy your opponent sent you, which you can now send back.";

    // ------------------------------------------------------------------ battlefield

    static final String BATTLEFIELD_1 = "Each player has **three rows**: **Close Combat** (sword), **Ranged Combat** (bow) and **Siege Combat** (catapult). Your rows are on the lower half of the board and your opponent's on the upper half, with the two Close Combat rows facing each other in the middle. A unit can only be played to the row its icon shows; **agile** units may choose between Close and Ranged Combat.";
    static final String BATTLEFIELD_2 = "Left of every row is a **Commander's Horn slot**. A horn placed there doubles the strength of that row's units (one horn per row).";
    static final String BATTLEFIELD_3 = "The **weather zone** holds weather cards. Weather always hits the matching rows on **both sides** of the board.";
    static final String BATTLEFIELD_4 = "Every row shows its total strength, and **each player's total** adds up all three rows — that is the number that decides the round.";
    static final String BATTLEFIELD_EXAMPLE = "Example: the opponent's rows add up to 35 and yours to 29 — right now the opponent is winning the round.";

    // ------------------------------------------------------------------ card types

    static final String TYPES_INTRO = "There are four kinds of cards:";
    static final String TYPES_UNIT = "**Unit cards** are your soldiers and monsters. They have a **strength** number and a **row** icon, and many have an **ability**.";
    static final String TYPES_HERO = "**Hero cards** are legendary units such as Geralt or Ciri. Heroes are **immune to everything**: weather, Scorch, Commander's Horn, Morale Boost, Decoy and Medic. Their strength never changes.";
    static final String TYPES_SPECIAL = "**Special cards** have no strength. They have a one-time effect: doubling a row (Commander's Horn), taking a unit back (Decoy) or burning the strongest units (Scorch).";
    static final String TYPES_WEATHER = "**Weather cards** go to the weather zone and drop **every non-hero unit** in their row to strength **1** — on both sides. Clear Weather removes all weather.";

    // ------------------------------------------------------------------ abilities

    static final String ABILITIES_INTRO = "Many units have an ability, shown by an icon on the card. Most trigger the moment you play the card (Spy, Medic, Muster, Scorch); others work as long as the unit stays on the battlefield (Morale Boost, Tight Bond, Commander's Horn).";
    static final String HERO_TEXT = "Not affected by special cards, weather or abilities — a hero's strength never changes.";
    static final String HERO_TIP = "Heroes can't be revived by a Medic, taken back by a Decoy or burned by Scorch.";
    static final String AGILE_TEXT = "Can be placed in either the Close Combat or the Ranged Combat row; you choose when you play it. It can't be moved afterwards.";
    static final String AGILE_TIP = "Revived by a Medic, an agile unit goes to whichever of its rows makes it strongest. Only the leader Francesca Findabair: Hope of the Aen Seidhe can move agile units.";

    // ------------------------------------------------------------------ special cards

    static final String SPECIALS_INTRO = "Special cards are played from your hand like any other card, but they have no strength.";
    static final String WEATHER_INTRO = "Weather cards go to the weather zone and stay there until Clear Weather is played or the round ends.";

    // ------------------------------------------------------------------ factions

    static final String FACTIONS_INTRO = "You play one of five factions. Your deck holds the faction's own cards plus all **neutral cards**, and every faction has a passive **perk** that works for the whole game.";
    static final String FACTIONS_NEUTRAL = "**Neutral cards** — heroes such as Geralt, Ciri and Yennefer, plus all special and weather cards — can be used by every faction.";

    // ------------------------------------------------------------------ leaders

    static final String LEADERS_1 = "Every army is led by a **leader card** from its faction. You choose it when you start a game, or let chance decide.";
    static final String LEADERS_2 = "Most leaders have an **active ability**: you can use it **once per game**, in any round, and using it **takes your turn**, just like playing a card. Click your leader card to use it. An ability that would have no effect right now can't be activated, so you can't waste it by accident.";
    static final String LEADERS_3 = "Some leaders have a **passive ability** instead: it is **always on**, for the whole game, and there is nothing to click.";
    static final String LEADERS_EXAMPLES = "A few examples:";
    static final String LEADERS_GALLERY = "The **Card Gallery** shows every leader with its ability.";

    // ------------------------------------------------------------------ starting the game

    static final String START_DEAL = "**Deal.** Both decks are shuffled and each player draws **10 cards**.";
    static final String START_REDRAW = "**Redraw.** You may swap up to **2 cards**, one at a time: the card goes to the bottom of your deck and you draw a new one. Swap away cards you are unlikely to use.";
    static final String START_FIRST = "**Who goes first?** A coin toss decides. If exactly one player plays **Scoia'tael**, that player chooses instead.";
    static final String START_BATTLE = "**Battle!** Round 1 begins, and the players take turns until both have passed.";
    static final String START_LEADERS = "Some passive leaders act right at the start: Francesca Findabair: Daisy of the Valley draws one extra card, and Emhyr var Emreis: The White Flame cancels the enemy leader's ability.";

    // ------------------------------------------------------------------ this version

    static final String OPTIONS_1 = "In The Witcher 3 you win cards by playing Gwent all over the world and build your own deck from them (at least 22 unit cards, at most 10 special cards). This version **skips deck building**: your deck is simply **every card of your faction plus every neutral card** you have — units, heroes, special and weather cards, each as many times as it exists in the original game. The computer's deck is built the same way, from the same cards.";
    static final String OPTIONS_MODES = "There are two ways to play:";
    static final String OPTIONS_FREE_PLAY = "**Free Play**: you have **every card** from the start.";
    static final String OPTIONS_COLLECTOR = "**Collector Mode**: you start with the **starter cards** — units of strength 4 or less and the weather cards — and **every win unlocks one more card** with all its copies: a unit of strength 5 to 7 on Easy, a strong card on Normal, a hero on Hard. **My Collection**, on the setup screen, shows the cards you own.";
    static final String OPTIONS_SETTINGS = "In both, you can change:";
    static final String OPTIONS_HAND = "**Starting hand**: how many cards are dealt at the start — **10** in the original game, up to 15 here. More cards mean longer rounds and bigger combos.";
    static final String OPTIONS_REDRAWS = "**Redraws**: how many cards you may swap after the deal — **2** in the original game, 0 to 6 here.";
    static final String OPTIONS_DIFFICULTY = "**Difficulty**: how well the computer plays — Easy, Normal or Hard.";
    static final String OPTIONS_2 = "You set all of this when you start **Free Play** or **Collector Mode** from the main menu. The setup screen remembers your choices until you quit.";

    // ------------------------------------------------------------------ controls

    static final String[] CONTROL_KEYS = {"Click a card in your hand", "Click a highlighted row", "Right-click / Esc", "Click your leader", "Pass button", "Choice windows", "Hover over a card", "F11"};
    static final String[] CONTROL_TEXTS = {"Selects it. The rows or targets where it can be played light up.", "Plays the selected card there (for a Decoy: click the unit to take back).", "Cancels the selection.", "Uses your leader ability (only when it can do something).", "Passes for the rest of the round.", "Medic and some leaders show cards to choose from — click the one you want.", "Shows it large, with its full rules text, in the info panel.", "Toggles full screen."};

    private RulesText() {
    }

    /** Extra explanation for a unit ability, or null. */
    static String abilityTip(Ability ability) {
        switch (ability) {
            case MEDIC:
                return "A revived Medic can revive another card, and a revived Spy draws you 2 more cards.";
            case MORALE_BOOST:
                return "Best in a row that already holds many units.";
            case MUSTER:
                return "Companions are cards with the same name or of the same family: one Crone brings the other two Crones, one Vampire brings all the Vampires.";
            case SPY:
                return "The spy's strength helps your opponent, but two new cards are almost always worth it.";
            case TIGHT_BOND:
                return "Two copies side by side are each worth double, three copies each worth triple.";
            case SCORCH:
                return "Hits both sides of the board — make sure the strongest unit isn't yours. Heroes are immune.";
            case SCORCH_CLOSE:
            case SCORCH_RANGED:
            case SCORCH_SIEGE:
                return "Only hits your opponent's row, and only when that row totals 10 or more. Heroes are immune.";
            case HORN:
                return "Works like a Commander's Horn card, but the card is a unit itself. A row can only be doubled once.";
            case BERSERKER:
                return "Skellige. Plays like a normal unit until a Mardroeme reaches its row.";
            case MARDROEME:
                return "Skellige. Works like the Mardroeme special card, but the card is a unit itself.";
            case AVENGER:
                return "The new card arrives when this unit is destroyed or when the round ends and the board is cleared.";
            default:
                return null;
        }
    }

    /** Extra explanation for a special or weather card, or null. */
    static String specialTip(SpecialType type) {
        switch (type) {
            case COMMANDERS_HORN:
                return "Goes into the horn slot to the left of one of your rows. Heroes are not doubled.";
            case DECOY:
                return "Only works on non-hero units on your own side. Great for re-using a Medic or a Spy, or for saving a unit.";
            case SCORCH:
                return "Hits both sides; if several units share the highest strength, all of them die. Heroes are immune — but when nothing stronger is on the board, even 0-strength units burn.";
            case MARDROEME:
                return "Skellige. Play it on a row with Berserkers to turn them into bears.";
            case BITING_FROST:
            case IMPENETRABLE_FOG:
            case TORRENTIAL_RAIN:
                return "Hits both players; heroes keep their strength. Best when your opponent has more in that row than you.";
            case SKELLIGE_STORM:
                return "Hits two rows on both sides at once; heroes keep their strength.";
            case CLEAR_WEATHER:
                return "Use it when the weather hurts you more than your opponent.";
            default:
                return null;
        }
    }

    /** How a faction plays, plus details of its perk. */
    static String factionTip(Faction faction) {
        switch (faction) {
            case NORTHERN_REALMS:
                return "Plays with Tight Bond infantry and strong Siege engines. Winning rounds keeps your hand full.";
            case NILFGAARD:
                return "Plays with spies, medics and strong heroes. A drawn round counts as your win.";
            case SCOIATAEL:
                return "Plays with many agile units and Muster groups. Going second lets you react to your opponent.";
            case MONSTERS:
                return "Plays with big Muster swarms that flood the board. The unit that stays is a random non-hero unit.";
            case SKELLIGE:
                return "Plays with Berserkers, Mardroeme and graveyard tricks. The two returning cards are random non-hero units, which makes round three especially strong.";
            default:
                return null;
        }
    }
}
