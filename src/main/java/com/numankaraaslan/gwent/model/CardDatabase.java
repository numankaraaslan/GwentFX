package com.numankaraaslan.gwent.model;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All card and leader definitions, loaded once from the JSON resources in {@code /com/numankaraaslan/gwent/data/}.
 * Immutable after loading, so it can be shared between the engine thread and the JavaFX thread.
 */
public final class CardDatabase
{

    private static final String CARDS_RESOURCE = "/com/numankaraaslan/gwent/data/cards.json";
    private static final String LEADERS_RESOURCE = "/com/numankaraaslan/gwent/data/leaders.json";

    private static CardDatabase instance;

    private final Map<String, CardDef> cardsById;
    private final Map<String, LeaderDef> leadersById;

    private CardDatabase(Map<String, CardDef> cardsById, Map<String, LeaderDef> leadersById)
    {
        this.cardsById = Collections.unmodifiableMap(cardsById);
        this.leadersById = Collections.unmodifiableMap(leadersById);
    }

    /** The shared database, loaded on first use. */
    public static synchronized CardDatabase get()
    {
        if (instance == null)
        {
            instance = load();
        }
        return instance;
    }

    /** Definition with the given id; throws if it doesn't exist. */
    public CardDef card(String id)
    {
        CardDef card = cardsById.get(id);
        if (card == null)
        {
            throw new IllegalArgumentException("Unknown card id: " + id);
        }
        return card;
    }

    /** Definition with the given id, or null. */
    public CardDef findCard(String id)
    {
        return cardsById.get(id);
    }

    /** Every card, including tokens, in file order. */
    public List<CardDef> cards()
    {
        return new ArrayList<>(cardsById.values());
    }

    /** Cards that belong to exactly this faction (tokens included), in file order. */
    public List<CardDef> cards(Faction faction)
    {
        List<CardDef> result = new ArrayList<>();
        for (CardDef card : cardsById.values())
        {
            if (card.faction() == faction)
            {
                result.add(card);
            }
        }
        return result;
    }

    /** Unique cards a deck of this faction is built from: the faction's cards plus neutral cards, without tokens. */
    public List<CardDef> deckPool(Faction faction)
    {
        List<CardDef> result = new ArrayList<>();
        for (CardDef card : cardsById.values())
        {
            if (!card.token() && (card.faction() == faction || card.faction() == Faction.NEUTRAL))
            {
                result.add(card);
            }
        }
        return result;
    }

    public List<LeaderDef> leaders()
    {
        return new ArrayList<>(leadersById.values());
    }

    public List<LeaderDef> leaders(Faction faction)
    {
        List<LeaderDef> result = new ArrayList<>();
        for (LeaderDef leader : leadersById.values())
        {
            if (leader.faction() == faction)
            {
                result.add(leader);
            }
        }
        return result;
    }

    /** Leader with the given id; throws if it doesn't exist. */
    public LeaderDef leader(String id)
    {
        LeaderDef leader = leadersById.get(id);
        if (leader == null)
        {
            throw new IllegalArgumentException("Unknown leader id: " + id);
        }
        return leader;
    }

    // ------------------------------------------------------------------ loading

    private static CardDatabase load()
    {
        Gson gson = new Gson();
        CardJson[] cardJsons = read(gson, CARDS_RESOURCE, CardJson[].class);
        LeaderJson[] leaderJsons = read(gson, LEADERS_RESOURCE, LeaderJson[].class);
        Map<String, CardDef> cards = new LinkedHashMap<>();
        for (CardJson json : cardJsons)
        {
            CardDef card = toCard(json);
            if (cards.put(card.id(), card) != null)
            {
                throw new IllegalStateException("Duplicate card id in cards.json: " + card.id());
            }
        }
        Map<String, LeaderDef> leaders = new LinkedHashMap<>();
        for (LeaderJson json : leaderJsons)
        {
            LeaderDef leader = toLeader(json);
            if (leaders.put(leader.id(), leader) != null)
            {
                throw new IllegalStateException("Duplicate leader id in leaders.json: " + leader.id());
            }
        }
        validateReferences(cards);
        return new CardDatabase(cards, leaders);
    }

    private static <T> T read(Gson gson, String resource, Class<T> type)
    {
        InputStream stream = CardDatabase.class.getResourceAsStream(resource);
        if (stream == null)
        {
            throw new IllegalStateException("Missing resource " + resource);
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
        {
            T result = gson.fromJson(reader, type);
            if (result == null)
            {
                throw new IllegalStateException("Empty resource " + resource);
            }
            return result;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot read " + resource, e);
        }
    }

    private static CardDef toCard(CardJson json)
    {
        String id = require(json.id, "card id");
        Set<RowType> rows = EnumSet.noneOf(RowType.class);
        if (json.rows != null)
        {
            for (String row : json.rows)
            {
                rows.add(parse(RowType.class, row, id));
            }
        }
        CardType type = parse(CardType.class, json.type, id);
        Ability ability = isBlank(json.ability) ? Ability.NONE : parse(Ability.class, json.ability, id);
        SpecialType special = isBlank(json.special) ? SpecialType.NONE : parse(SpecialType.class, json.special, id);
        Expansion expansion = isBlank(json.expansion) ? Expansion.BASE : parse(Expansion.class, json.expansion, id);
        if (type == CardType.UNIT && rows.isEmpty())
        {
            throw new IllegalStateException("Unit card without rows: " + id);
        }
        return new CardDef(id, require(json.name, "name of " + id), parse(Faction.class, json.faction, id), type, json.hero, Collections.unmodifiableSet(rows), json.strength, ability, special, json.copies, text(json.group), text(json.summons), text(json.transformsTo), text(json.avengerSummons), json.token, expansion);
    }

    private static LeaderDef toLeader(LeaderJson json)
    {
        String id = require(json.id, "leader id");
        Expansion expansion = isBlank(json.expansion) ? Expansion.BASE : parse(Expansion.class, json.expansion, id);
        return new LeaderDef(id, require(json.name, "name of " + id), parse(Faction.class, json.faction, id), parse(LeaderAbility.class, json.ability, id), text(json.description), expansion);
    }

    private static void validateReferences(Map<String, CardDef> cards)
    {
        for (CardDef card : cards.values())
        {
            if (!card.transformsTo().isEmpty() && !cards.containsKey(card.transformsTo()))
            {
                throw new IllegalStateException(card.id() + " transforms into unknown card " + card.transformsTo());
            }
            if (!card.avengerSummons().isEmpty() && !cards.containsKey(card.avengerSummons()))
            {
                throw new IllegalStateException(card.id() + " summons unknown card " + card.avengerSummons());
            }
        }
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, String cardId)
    {
        if (isBlank(value))
        {
            throw new IllegalStateException("Missing " + type.getSimpleName() + " for " + cardId);
        }
        try
        {
            return Enum.valueOf(type, value.trim().toUpperCase());
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalStateException("Unknown " + type.getSimpleName() + " '" + value + "' for " + cardId, e);
        }
    }

    private static String require(String value, String what)
    {
        if (isBlank(value))
        {
            throw new IllegalStateException("Missing " + what);
        }
        return value.trim();
    }

    private static String text(String value)
    {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    /** JSON shape of a card (filled by Gson). */
    private static final class CardJson
    {
        private String id;
        private String name;
        private String faction;
        private String type;
        private boolean hero;
        private List<String> rows;
        private int strength;
        private String ability;
        private String special;
        private int copies;
        private String group;
        private String summons;
        private String transformsTo;
        private String avengerSummons;
        private boolean token;
        private String expansion;
    }

    /** JSON shape of a leader (filled by Gson). */
    private static final class LeaderJson
    {
        private String id;
        private String name;
        private String faction;
        private String ability;
        private String description;
        private String expansion;
    }
}
