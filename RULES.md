# Rules notes

This remake follows the Gwent of The Witcher 3, including Hearts of Stone and Blood and Wine (which added the
Skellige faction). The card texts leave many details open; this file lists how this remake handles them.

The rules are based on community sources about the game, mainly the Witcher wiki and existing fan remakes. Most
base-game rules were confirmed by more than one source. Some Hearts of Stone and Blood and Wine details rest on the
wiki alone; those are marked *(wiki)* and are the most likely to differ from the real game.

## Cards and strength

- **Card data** (`cards.json`): 162 entries, 22 leaders. Card counts follow the wiki. A few counts are
  debatable: Poor F***ing Infantry ×3 (some remakes use 4), and Catapult ×2. Avallac'h appears under
  his in-game card name, "Mysterious Elf". Roach (a next-gen patch reward) and cards cut from the game
  are not included.
- **Strength order**: weather → tight bond (× number of same-name cards in the row) → morale boost
  (+1 per other booster) → horn (×2) → spy doubling (Eredin: The Treacherous). Heroes are never affected.
- **Weather** sets non-hero units in the affected rows to 1; units with 0 strength stay at 0. A weather
  card that is already active is simply discarded. With King Bran, your own units lose only half their
  strength, rounded so that 5 becomes 3. *(wiki; the rounding is unknown)*
- **Commander's Horn units** (Dandelion, Draig Bon-Dhu) double every other unit in their row, but not
  themselves. A Commander's Horn card or a horn leader in the same row doubles them too. Horns never stack.
- **Horn-type leader abilities** (Siegemaster, The Beautiful, Commander of the Red Riders) put a horn in the
  row's special slot for the rest of the round. They can only be used if that slot is empty.
- **Mardroeme** occupies the row's special slot, so a row has either a horn or Mardroeme. Berserkers in
  that row transform right away, and so do berserkers that arrive later in the round. Ermion acts as a
  Mardroeme for his own row without using the slot. *(wiki)* A transformed bear turns back into its
  Berserker card when it leaves the battlefield (destroyed, round end, or Decoy).
- **Summoned tokens** (Hemdall, Bovine Defense Force) exist only on the battlefield. Medic, Skellige and the
  discard-pile leaders can't bring them back, and Crach an Craite's shuffle removes them from the game.

## Abilities

- **Scorch** kills every non-hero unit tied for the highest strength, on both sides. If nothing stronger
  is on the board, that includes 0-strength units. Clan Dimun Pirate can burn itself.
- **Row scorch** (Villentretenmerth, Toad, Schirrú and the scorch leaders) needs the enemy row's total,
  heroes included, to be at least 10.
- **Muster** pulls the matching cards from your **deck and your hand**. The card text only says
  "deck", but the original game does both. Arachas Behemoth musters the Arachas; an Arachas doesn't muster
  the Behemoth. Gaunter O'Dimm musters the three Darkness cards.
- **Medic** chooses a non-hero unit from your discard pile. Revived spies, medics, muster and scorch units
  trigger again. An agile unit revived by a Medic is placed in its stronger row automatically, as in the
  original.
- **Decoy** targets a non-hero unit on your own side. That can be an enemy spy sitting on your side,
  which you can then replay as your own spy.
- **Avenger** (Kambi → Hemdall, Cow → Bovine Defense Force):
  - If the card is destroyed during a round, its replacement appears immediately.
  - If it is cleared at the end of a round, the replacement appears for the next round.
  - Being taken back with a Decoy doesn't count as being destroyed or cleared.

  *(wiki)*

## Leaders

- Each leader ability can be used once per game, and using it takes your turn.
- Like the original, an ability can only be activated when it would actually do something, so you
  can't waste it by accident. Examples:
  - Foltest: King of Temeria needs a Fog card in the deck, and no Fog already active.
  - King of the Wild Hunt only offers weather that would change something.
  - A row scorch needs an enemy row worth 10 or more.
- Bringer of Death and The Relentless only offer non-hero units, even though their text says "a card".
- Passive leaders work automatically:
  - The White Flame cancels the other leader, including Daisy's extra card.
  - Daisy of the Valley draws 1 extra starting card.
  - Invader of the North makes medics, for both players, revive a random unit.
  - The Treacherous doubles every spy.
  - King Bran halves weather damage.

## Faction perks

- Northern Realms draws 1 card after winning a round (not after a draw).
- Nilfgaard wins tied rounds. If both sides are Nilfgaard, a tie is a normal draw.
- Scoia'tael chooses who starts round 1. If both sides are Scoia'tael, a coin toss decides.
- Monsters keep one random **non-hero** unit on the battlefield after each round.
- Skellige puts 2 random **non-hero** units from its discard pile back on the battlefield when
  round 3 starts. Their abilities don't trigger. *(wiki)*

## Round flow

- As in the original, you're dealt 10 cards and may redraw up to 2 of them; both numbers can be changed on the
  setup screen. Each redrawn card goes to the bottom of the deck.
- Each player has 2 gems. A drawn round costs both players a gem, unless Nilfgaard wins the tie.
- A player with no legal move is passed automatically. Starting a round with an empty hand means
  sitting that round out.
- The winner of a round starts the next one; a Nilfgaard tie win counts as a win. After a real draw,
  the player who made the last move starts.

## Discard piles

Battlefield cards go to the discard pile of the side they were on, so a spy you played ends up in your opponent's
pile. Weather cards go back to whoever played them.

## Collector Mode

Collector Mode isn't in the original game; it brings back a little of its card collecting. There is still no deck
building: both decks hold every card of their faction plus every neutral card, but only the cards you own, with all
their copies. The computer plays with the same cards. You own the starter cards from the start, and every win
unlocks one more card, picked at random from the tier of the difficulty you beat:

| Tier | Cards | Cards to win |
| --- | --- | --- |
| Starter | Units of strength 4 or less, except spies, Medics, Scorch and Horn units. Every weather card, Clear Weather included. | owned from the start |
| Easy | Units of strength 5 to 7. | 47 |
| Normal | Units of strength 8 or more. Spies, Medics, Scorch units (row scorch included) and Horn units of any strength. The special cards: Commander's Horn, Decoy, Scorch, Mardroeme. | 26 |
| Hard | Heroes, whatever their ability. | 24 |

- A card is won with all its copies. The two Muster families with different names, the three Crones and the five
  Vampires, are won together as one card, in the highest tier of their members (Easy for both).
- A win picks only from its own tier, never a lower one. Once a tier is complete, wins at that difficulty unlock
  nothing. Losses and draws unlock nothing.
- 97 wins unlock everything. With only the starter cards, a deck holds 31–37 cards, depending on the faction.
- Weather cards are starter cards so that every leader ability works from the first game. Two passive leaders do
  nothing until their cards are won: Eredin Bréacc Glas: The Treacherous needs spies, and Emhyr var Emreis: Invader
  of the North needs Medics.
