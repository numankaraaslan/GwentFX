# How the AI makes decisions

The computer opponent (the "AI") uses no machine learning and no deep search. On each turn it:

1. **tries every move it can make** on a copy of the game, using the real rules code;
2. **scores each move**: the strength it gains now, plus the value of the cards it keeps for later rounds;
3. **picks a move using round tactics**: rules of thumb that depend on who has won a round, who has passed and
   who is ahead.

The code is in [`src/main/java/com/numankaraaslan/gwent/ai`](src/main/java/com/numankaraaslan/gwent/ai). The numbers
below are the current values in that code.

## A few Gwent terms

- **Gems**: each player has 2 lives. Losing a round costs one gem, and a drawn round costs both players one.
  A player who loses both gems loses the game. If both lose their last gem in the same round, the game is a draw.
- **Passing**: a player who passes can't play again this round. When both have passed, the higher total wins the
  round.
- **Card advantage**: having more cards in hand than the opponent. You don't get new cards between rounds, so
  winning a round by spending many cards can lose the game. Good players win rounds "cheaply" and save cards.
- **Spy**: a unit played on the opponent's side. It adds to their total, but its owner draws 2 cards, so the owner
  ends up with one card more.
- **Nilfgaard's perk**: a Nilfgaard player wins tied rounds against any other faction.

## Where the code is

| File | Role |
| --- | --- |
| [`AiController.java`](src/main/java/com/numankaraaslan/gwent/ai/AiController.java) | What the game engine talks to. Passes each decision to `AiBrain` and adds short pauses. |
| [`AiBrain.java`](src/main/java/com/numankaraaslan/gwent/ai/AiBrain.java) | The decisions: which move to play, round tactics, difficulty levels, redraws. |
| [`MoveSimulator.java`](src/main/java/com/numankaraaslan/gwent/ai/MoveSimulator.java) | Plays each possible move on a copy of the game and measures the result. |
| [`MoveEval.java`](src/main/java/com/numankaraaslan/gwent/ai/MoveEval.java) | The measured result of one move. |
| [`CardValues.java`](src/main/java/com/numankaraaslan/gwent/ai/CardValues.java) | How much each card is worth keeping in hand. |
| [`RoundContext.java`](src/main/java/com/numankaraaslan/gwent/ai/RoundContext.java) | The state of the round: gems, totals, hand sizes, who has passed, who wins ties. |
| [`ChoicePolicy.java`](src/main/java/com/numankaraaslan/gwent/ai/ChoicePolicy.java) | Choices in the middle of a move: Medic target, weather from the deck, discards, agile rows. |
| [`SimulationDecider.java`](src/main/java/com/numankaraaslan/gwent/ai/SimulationDecider.java) | Makes those choices instantly inside simulations, using `ChoicePolicy`. |

### How a turn reaches the AI

1. `GameScreen` creates the AI with the difficulty chosen on the setup screen: `new AiController(difficulty)`.
   `GameEngine` wraps it in `SafeController`, so an error in the AI makes it pass instead of crashing the game. The
   engine also gets the game mode's card pool, the cards both decks are built from: every card in Free Play, the
   cards you own in Collector Mode. The AI reads it from the game state.
2. On the AI's turn, `GameEngine.playRound` lists the legal moves (`Rules.legalMoves`). If passing is the only one,
   the AI passes automatically.
3. Otherwise the engine calls `chooseMove` and waits for the answer. This runs on the engine thread, not the UI
   thread.
4. `AiController` gets the move from `AiBrain.chooseMove`. It then waits until 0.65–1.2 s have passed, so you can
   follow the play.
5. The engine plays the move with `Resolver.applyMove`. An illegal answer is replaced by a pass.

## Step 1: try every move

For every card it could play, and for its leader ability, `MoveSimulator.evaluate` does this:

1. Copy the whole game state.
2. Shuffle both decks in the copy. The AI doesn't know the real order, so it can't rely on it.
3. Play the move on the copy with the real `Resolver`, the class that runs the actual game. Every ability (Muster,
   Scorch, Medic, Horn, weather...) is applied exactly. If the move needs a choice, like a Medic's target,
   `SimulationDecider` makes it the same way the AI would in the real game.
4. Measure the result (step 2).

Passing isn't simulated. The tactics in step 3 decide when to pass.

## Step 2: score each move

Each move gets two numbers.

**Gain**: how much the difference between the two totals changes.

- A unit of strength 5 gives +5.
- A spy of strength 1 gives −1, because it goes to the opponent's side.
- A Scorch card that kills your strongest unit, an 8, gives +8. If the AI also has an 8 on the board, that one dies
  too, and the gain is 0.

**Card potential**: what the move does to the AI's hand for later rounds, using the card values below.

- \+ the value of each card it gains. A card drawn from the deck counts as the average card left in its deck, since
  it can't know which one it will get.
- − the play cost of the card it plays: its keep value, except a spy costs 0 (it brings two cards) and a Decoy 3.
- − the value of any hand card lost in another way, for example discarded by a leader ability.
- − the value of the leader ability, if it uses it: 8, or 4 when it must win this round, or 0 in the final round
  (lower with weaker cards, see [Collector Mode](#collector-mode)).
- \+ for an Avenger it puts on the board, the value of the card that will replace it.

The **score** combines the two:

> score = gain + weight × card potential

The weight is how much saving cards matters at this point of the game (`RoundContext.conserve`):

| Situation | Weight |
| --- | --- |
| Round 1 (nobody has lost a round) | 0.9 |
| The AI has won a round | 0.8 |
| The AI has lost a round, so it must win this one | 0.35 |
| Final round (both players on their last gem) | 0: cards are worth nothing after this round |

On **Hard**, a move also loses points if it piles strength where a Scorch would hit. See
[Difficulty levels](#difficulty-levels).

### Card values

`CardValues.keepValue` estimates, in strength points, how much a card is worth keeping in hand:

| Card | Value |
| --- | --- |
| Unit without an ability | its strength |
| Spy | 10 − 0.3 × strength |
| Medic | strength + 0.8 × the best unit it could revive (3 if the discard pile has none) |
| Muster | strength + 0.85 × the total strength of the cards it would pull in |
| Tight bond | strength × (1 + 0.6 × the number of other copies still available, at most 2) |
| Morale boost | strength + 2 |
| Horn unit (Dandelion, Draig Bon-Dhu) | strength + 4 |
| Scorch unit (Villentretenmerth, Clan Dimun Pirate...) | strength + 3 |
| Berserker | strength + 5 if a Mardroeme is available, otherwise + 1 |
| Ermion | strength + 3 if it has berserkers |
| Avenger | strength + 0.7 × the strength of the card that replaces it |
| Hero | + 2 on top of the values above |
| Commander's Horn card | 8 |
| Decoy, Scorch card | 6 |
| Mardroeme card | 7 if it has berserkers, otherwise 2 |
| Weather card | 4 (Clear Weather: 3) |

The average card in a full deck is worth about 6.5–8, depending on the faction. With only Collector Mode's starter
cards, it's about 4.7–5.7.

### Example

Round 1 (weight 0.9), with the average card left in the deck worth 7:

| Move | Gain | Card potential | Score |
| --- | --- | --- | --- |
| Thaler (spy, strength 1) | −1 | 2 cards drawn × 7 = +14 | −1 + 0.9 × 14 = **11.6** |
| Commander's Horn on a row of units worth 12 | +12 | −8 | 12 − 0.9 × 8 = **4.8** |
| Ves (unit, strength 5) | +5 | −5 | 5 − 0.9 × 5 = **0.5** |
| Geralt (hero, strength 15) | +15 | −17 | 15 − 0.9 × 17 = **−0.3** |
| Commander's Horn on a row of units worth 4 | +4 | −8 | 4 − 0.9 × 8 = **−3.2** |

So in round 1 it plays spies first, keeps the Horn until a row is worth doubling, and holds Geralt back. In the final
round the weight is 0, so the score is just the gain: Geralt scores 15 and the spy −1.

## Step 3: pick a move

The budgets and leads in this step are the Free Play values. With weaker cards they shrink, see
[Collector Mode](#collector-mode).

`AiBrain.chooseMove` first works out the situation, in this order:

```text
nothing to play                       → pass
difficulty is Easy                    → easyMove
you have passed                       → afterOpponentPassed
final round (both on their last gem)  → finalRound
the AI has lost a round               → mustWin
the AI has won a round                → opponentMustWin
otherwise (round 1)                   → roundOne
```

**Ahead** means the round would be the AI's if it ended now. A tie is a win for a Nilfgaard player against another
faction. So for the AI, a tie is a win when it plays Nilfgaard, a loss when you play Nilfgaard, and a real draw
otherwise.

The tactics choose from the scored moves in a few ways:

| Term | Meaning |
| --- | --- |
| best move | the move with the highest score |
| biggest gain | the move that adds the most strength right now |
| cheapest winning move | of the moves that put the AI ahead, the one that spends the least card potential (on a tie, the one with the bigger lead) |
| card-gaining move | a move that leaves the AI with more cards in hand, like a spy or a Medic reviving a spy |
| last-resort draw | when nothing else helps: of the moves that add cards to its hand, the one with the best gain + card potential, hoping to draw something useful. This one may be a leader ability. |
| saving move | when giving up a round: the move that keeps the most card potential, at least 5 |
| free move | before passing: a move that costs neither strength nor card potential and still scores above 0, like an Avenger whose replacement arrives next round. Never in the final round. |

The leader ability is never used as a card-gaining, saving or free move.

### You have passed (`afterOpponentPassed`)

You can't play any more this round, so the AI's moves decide it.

- **AI ahead:**
  - In the final round, or when it has already won a round, it passes. That wins it the game.
  - Otherwise it first plays card-gaining moves that keep it ahead (free cards, since you can't answer), then
    passes.
- **AI not ahead, and losing this round would lose it the game** (final round, or it has already lost a round):
  1. the cheapest winning move, at any cost;
  2. else the biggest gain;
  3. else a last-resort draw, such as a spy, hoping for a winning card. It skips this when the final round stands at
     a real draw: it takes the drawn game rather than risk losing it;
  4. else it passes.
- **AI not ahead, and it has already won a round:**
  1. At a real draw it passes: a drawn round costs you your last gem, so it wins the game. When you play Nilfgaard,
     a tie is your win, so this doesn't apply.
  2. Otherwise the cheapest winning move, at any cost;
  3. else the biggest gain, if its cards can still close the gap;
  4. else it gives up the round.
- **AI not ahead, round 1:**
  1. the cheapest winning move that spends at most 8 potential (Ves, worth 5, is allowed; Geralt, worth 17, isn't);
  2. else, if it has more cards than you and hasn't given up this round before, the biggest gain within that
     budget, if two such cards could close the gap;
  3. else it gives up the round.

### Final round (`finalRound`)

Both players are on their last gem, and you haven't passed. The weight is 0, so a move's score is simply its gain.

- If it's ahead and you have nothing left to play (no cards and no leader ability), it passes.
- Otherwise it plays its best move, if that scores above 0.
- If nothing does: when it isn't ahead, it plays the biggest gain if any, else a last-resort draw; when it is ahead,
  it plays a last-resort draw only if its gain + card potential is above 0, else it passes.

### The AI has lost a round (`mustWin`)

Losing this round would lose the AI the game. You haven't passed.

- **Not ahead:** the cheapest winning move at any cost; else the biggest gain; else a last-resort draw; else it
  passes (nothing can help).
- **Ahead:** first card-gaining moves that keep it ahead. It passes once the lead is safe: you have nothing left to
  play, or it leads by 25 or more, or by 15 or more while you hold at most one card. Until then it adds the
  cheapest card that gains strength.

### The AI has won a round (`opponentMustWin`)

You must win this round or lose the game. You haven't passed.

- **Ahead:** card-gaining moves that keep it ahead, else it passes. To win the round you now have to spend cards,
  while it keeps its own for round 3.
- **Not ahead:** a card-gaining move first; else, if it has more cards than you, the cheapest winning move that
  spends at most 10 potential; else it gives up the round and saves its cards for round 3.

### Round 1 (`roundOne`)

Nobody has lost a round yet, and you haven't passed. The aim is to win round 1 only if that doesn't cost card
advantage.

The AI limits how much it spends in this round: at most max(3, starting hand ÷ 3) cards (3 with a 10-card hand),
and no more once its hand is down to max(3, starting hand ÷ 2 − 1) cards. Card-gaining moves aren't limited.

- **Ahead, with cards on the board:**
  1. card-gaining moves that keep it ahead;
  2. else, with at least as many cards as you, it passes. You then have to spend cards to win the round, or give it
     up;
  3. else (fewer cards than you), its best move if it scores 2 or more and the limit allows it;
  4. else it passes.
- **Not ahead, or nothing on the board yet:**
  1. a card-gaining move;
  2. else the cheapest move that takes the lead, if it spends at most 8 potential, scores 0 or more and the limit
     allows it;
  3. else, with at least 2 more cards than you, its best move if it scores above 0 and the limit allows it;
  4. else it gives up the round.

"Nothing on the board yet" matters for a Nilfgaard AI: it wins a 0–0 tie, but passing on an empty board would hand
you the round for a single card.

### Giving up a round (`concede`)

When the AI gives up a round that doesn't decide the game:

1. It remembers this, so it won't start chasing that round with several cards later, even if you pass.
2. If the round is lost as it stands (behind, or tied while you play Nilfgaard) and it isn't the final round, it first
   plays a saving move, such as a spy, a Decoy taking back a strong unit, or an Avenger. What that does to the
   round's total no longer matters.
3. Then it passes, unless a free move is available.

## Difficulty levels

| Level | Behavior |
| --- | --- |
| Easy | `easyMove` replaces step 3 (below). It never redraws. |
| Normal | Steps 1–3 as described above. |
| Hard | Normal, plus Scorch caution (below). |

**Easy** (`easyMove`):

- If you have passed and it's ahead, it passes.
- Outside the final round and its must-win rounds, it passes early: half the time once you've passed, and 60% of
  the time when it leads by 6 or more.
- 35% of the time it plays a random sensible move: one that doesn't lose strength, or that gains cards.
- Otherwise it plays the biggest gain, and passes if nothing gains strength.

**Scorch caution (Hard).** A Scorch kills the strongest units on the board. The AI's *exposure* is how much more
strength a Scorch would kill on its side than on yours (never below 0). While you still have cards and haven't
passed, each move's score loses risk × (exposure after the move − exposure before). The risk is 0.2, or 0.8 when
Emperor of Nilfgaard showed it a Scorch in your hand that hasn't been played since. So it avoids turning one big unit
into an obvious Scorch target. The risk is 0 when your deck can't hold a Scorch at all: neither the Scorch card nor,
for Skellige, Clan Dimun Pirate is in it. That happens in Collector Mode until one of them is unlocked.

## Collector Mode

In Collector Mode both decks are built from the cards you own, so in your first games they hold only the starter
cards: units of strength 4 or less and the weather cards. The card values need no change, since they come from the
cards themselves, and a drawn card counts as the average card left in the AI's own deck. But the fixed amounts in
step 3 were tuned for the full collection. A lead of 25 is about 5 average units of the full collection, but 9 starter
units.

So the AI multiplies those amounts by the **card scale** (`RoundContext.cardScale`): the average strength of the
units both decks are built from, divided by that of all units. You know these cards too: they are your collection.

| Cards owned | Card scale |
| --- | --- |
| Every card (Free Play) | 1, so nothing changes |
| Starter cards only | 0.56 (average unit strength 2.69 instead of 4.82) |
| Starter + every Easy card | 0.83 |
| Starter + every Easy and Normal card | 0.87 |

With only the starter cards, the round 1 budgets become 4.5 instead of 8, the safe lead in a must-win round 14 instead
of 25, and the leader ability is worth 4.5 instead of 8.

- **Scaled:** the spending budgets (8, 8 and 10), the safe leads (25 and 15), the score needed to play on in round 1
  (2), the least potential of a saving move (5), Easy's passing lead (6) and the leader's value (8 and 4).
- **Not scaled:** the card values, the weights, the chances (Easy's random moves, Hard's Scorch risk) and the limits
  on how many cards it spends.

## Other decisions

**Redraw** (`AiBrain.chooseRedraw`, Normal and Hard). It looks for its least useful card. Each card counts at its keep
value, except:

- an extra copy of a muster group counts as 0.5. When the kept copy is played, it pulls in the swapped one from the
  deck too, so the swap costs nothing;
- a second copy of the same weather card counts as 1, and a third or later weather card at most 2.

If the lowest value is more than 0.5 below the average card left in its deck, it swaps that card. The engine asks
again after each swap, until the AI stops or runs out of redraws.

**Choices during a move** (`ChoicePolicy`). The same rules are used inside the simulations:

| Choice | How it picks |
| --- | --- |
| Medic: which unit to revive | The unit worth most right now. A spy is worth 2 draws × the average deck card (at least 4) − its strength. Any other unit is worth its strength in its best row, plus what its ability would do now (Muster: the cards it pulls in; Scorch: the strength it would kill; Horn: the row's total...). |
| Eredin, King of the Wild Hunt: which weather card to play from the deck | The one that leaves the best difference between the totals. |
| Eredin, Destroyer of Worlds: which 2 cards to discard | The 2 with the lowest keep value. |
| Taking a card (Eredin, Bringer of Death; Emhyr, The Relentless; Destroyer of Worlds' draw) | The one with the highest keep value. |
| Agile unit: which row | The row where the card is strongest. |

**Who goes first** (Scoia'tael perk). When the AI plays Scoia'tael against another faction, it always lets you go
first, so it can answer your first move.

**Pacing** (`AiController`). It pauses 0.65–1.2 s before each move, 0.45 s for a choice during a move, 0.3 s per
redraw and 0.4 s for the Scoia'tael choice.

## What the AI knows

It plays fair:

- It sees what you see: the board, weather, discard piles, totals, gems, both leaders, and how many cards you hold.
- It knows its own hand and which cards are left in its own deck, but not their order: the simulations shuffle the
  decks.
- It knows which cards the decks are built from: every card in Free Play, your collection in Collector Mode. It
  doesn't know which of them you hold.
- It never looks at your hand or your deck. The one exception: when its leader, Emperor of Nilfgaard, shows it 3
  cards from your hand, it remembers them. Hard uses this to watch for a Scorch.

## Limits

- **One move deep.** It simulates its own next move, never your answer to it. The round tactics stand in for looking
  further ahead.
- **No card counting.** It doesn't work out which cards you might still hold. For example, Hard keeps assuming a 20%
  Scorch risk even after all three Scorch cards have been played. It only rules a Scorch out when your deck can't
  hold one at all.
- **Hand-tuned numbers.** The card values, weights and budgets are reasonable guesses, not learned or proven.

## Tuning

| To change | Look at |
| --- | --- |
| The pauses | constants at the top of `AiController` |
| Easy's randomness, Hard's Scorch risk, spending budgets, safe leads | constants at the top of `AiBrain` (below) |
| What cards are worth | `CardValues.keepValue` and `CardValues.playCost` |
| How much saving cards matters, the leader's value, the round 1 hand floor | `RoundContext.conserve`, `leaderValue`, `roundOneHandFloor` |
| How the amounts shrink with weaker cards | `RoundContext.cardScale` and `scaled` |
| Choices during a move | `ChoicePolicy` |

The constants at the top of `AiBrain`. All but the chances are Free Play values, multiplied by the card scale when
they are used:

| Constant | Value | Meaning |
| --- | --- | --- |
| `EASY_RANDOM_MOVE` | 0.35 | Chance that Easy plays a random sensible move. |
| `EASY_PASS_LEAD` | 6 | Lead at which Easy may pass early. |
| `SCORCH_RISK_UNKNOWN` | 0.2 | Hard's Scorch risk without any information. |
| `SCORCH_RISK_REVEALED` | 0.8 | Hard's Scorch risk after seeing a Scorch in your hand. |
| `ROUND_ONE_CHASE_BUDGET` | 8 | Most potential it spends per move to win round 1 after you passed. |
| `ROUND_ONE_RETAKE_BUDGET` | 8 | Most potential it spends to retake the lead in round 1 while you still play. |
| `OPPONENT_MUST_WIN_BUDGET` | 10 | Most potential it spends per exchange in a round you must win. |
| `MUST_WIN_SAFE_LEAD` | 25 | Lead at which it stops adding cards in a round it must win. |
| `MUST_WIN_SAFE_LEAD_LAST_CARD` | 15 | The same, when you hold at most one card. |
| `ROUND_ONE_PUSH_MIN_SCORE` | 2 | Least score of a move it plays in round 1 while ahead with fewer cards than you. |
| `SAVE_MIN_RESOURCES` | 5 | Least potential a saving move must keep. |
