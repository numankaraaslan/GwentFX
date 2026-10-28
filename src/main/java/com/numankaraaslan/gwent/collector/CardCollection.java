package com.numankaraaslan.gwent.collector;

import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.CardPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The cards you own in Collector Mode: every starter card plus the rewards unlocked so far. The computer's deck is
 * built from the same cards. {@link CollectionFile} reads and saves it.
 */
public final class CardCollection {

    private final List<Reward> rewards;
    private final Map<String, Reward> rewardOfCard;
    private final Set<String> unlocked;

    private CardCollection(List<Reward> rewards, Set<String> unlockedIds) {
        this.rewards = rewards;
        this.rewardOfCard = new HashMap<>();
        for (Reward reward : rewards) {
            for (CardDef card : reward.cards()) {
                rewardOfCard.put(card.id(), reward);
            }
        }
        this.unlocked = new HashSet<>();
        for (String id : unlockedIds) {
            Reward reward = rewardOfCard.get(id);
            if (reward != null && reward.tier() != Tier.STARTER) {
                unlocked.add(id);
            }
        }
    }

    /** A collection with these cards unlocked; ids of unknown cards, tokens and starter cards are ignored. */
    public static CardCollection of(CardDatabase db, Set<String> unlockedIds) {
        return new CardCollection(buildRewards(db), unlockedIds);
    }

    /** Every reward, the starter ones included, in file order. */
    public List<Reward> rewards() {
        return rewards;
    }

    /** The tier of the reward this card belongs to; null for tokens and unknown cards. */
    public Tier tierOf(CardDef card) {
        Reward reward = rewardOfCard.get(card.id());
        return reward == null ? null : reward.tier();
    }

    /** How many rewards this tier has in total, owned or not. */
    public int rewardCount(Tier tier) {
        int count = 0;
        for (Reward reward : rewards) {
            if (reward.tier() == tier) {
                count++;
            }
        }
        return count;
    }

    /** Whether you own this card: starter cards always, the others once unlocked. */
    public boolean owns(CardDef card) {
        Reward reward = rewardOfCard.get(card.id());
        return reward != null && (reward.tier() == Tier.STARTER || unlocked.contains(card.id()));
    }

    /** The cards both decks are built from: every card you own. */
    public CardPool pool() {
        List<CardDef> owned = new ArrayList<>();
        for (Reward reward : rewards) {
            for (CardDef card : reward.cards()) {
                if (owns(card)) {
                    owned.add(card);
                }
            }
        }
        return CardPool.of(owned);
    }

    /** The rewards of this tier you don't fully own yet. */
    public List<Reward> remaining(Tier tier) {
        List<Reward> result = new ArrayList<>();
        for (Reward reward : rewards) {
            if (reward.tier() == tier && !ownsAll(reward)) {
                result.add(reward);
            }
        }
        return result;
    }

    /** Unlocks a random reward of this tier that you don't own yet and returns it; null when you own them all. */
    public Reward unlockRandom(Tier tier, Random random) {
        List<Reward> candidates = remaining(tier);
        if (candidates.isEmpty()) {
            return null;
        }
        Reward reward = candidates.get(random.nextInt(candidates.size()));
        for (CardDef card : reward.cards()) {
            unlocked.add(card.id());
        }
        return reward;
    }

    /** Also unlocks every card the other collection has unlocked. */
    public void addUnlocked(CardCollection other) {
        unlocked.addAll(other.unlocked);
    }

    /** Ids of the unlocked cards in file order. Starter cards aren't listed: they are always owned. */
    public List<String> unlockedIds() {
        List<String> ids = new ArrayList<>();
        for (Reward reward : rewards) {
            for (CardDef card : reward.cards()) {
                if (unlocked.contains(card.id())) {
                    ids.add(card.id());
                }
            }
        }
        return ids;
    }

    private boolean ownsAll(Reward reward) {
        for (CardDef card : reward.cards()) {
            if (!owns(card)) {
                return false;
            }
        }
        return true;
    }

    /** One reward per card, except that the cards of a Muster group are one reward together. Tokens are left out. */
    private static List<Reward> buildRewards(CardDatabase db) {
        Map<String, List<CardDef>> groups = new LinkedHashMap<>();
        for (CardDef card : db.cards()) {
            if (card.token()) {
                continue;
            }
            String key = card.group().isEmpty() ? "card:" + card.id() : "group:" + card.group();
            List<CardDef> cards = groups.get(key);
            if (cards == null) {
                cards = new ArrayList<>();
                groups.put(key, cards);
            }
            cards.add(card);
        }
        List<Reward> rewards = new ArrayList<>();
        for (List<CardDef> cards : groups.values()) {
            Tier tier = Tier.STARTER;
            for (CardDef card : cards) {
                Tier own = Tier.of(card);
                if (own.compareTo(tier) > 0) {
                    tier = own;
                }
            }
            rewards.add(new Reward(Collections.unmodifiableList(cards), tier));
        }
        return Collections.unmodifiableList(rewards);
    }
}
