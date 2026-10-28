package com.numankaraaslan.gwent.collector;

import com.numankaraaslan.gwent.model.CardDatabase;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Collector Mode save file: a text file in the user's home folder ({@code .gwentfx/collection.txt}) with one
 * unlocked card id per line. Lines starting with # are comments. Editing the file by hand is fine: unknown ids are
 * ignored, and deleting the file starts the collection over. A problem with the file never stops the game, so nothing
 * here throws: a missing file is created, a file that can't be read counts as one with no cards, and a save that fails
 * leaves the old file as it was.
 */
public final class CollectionFile
{

    private static final Path FILE = Paths.get(System.getProperty("user.home"), ".gwentfx", "collection.txt");
    /** A save is written here first and then moved over the file in one step. */
    private static final Path TEMP = FILE.resolveSibling(FILE.getFileName() + ".tmp");
    private static final String[] HEADER = { "# GwentFX Collector Mode: the cards you have unlocked, one card id per line.", "# Starter cards aren't listed: you always own them. Delete this file to start over." };
    /** Byte order mark that some editors put at the start of a text file. */
    private static final String BOM = "﻿";

    private CollectionFile()
    {
    }

    /** Where the collection is saved. */
    public static Path path()
    {
        return FILE;
    }

    /** Whether the file is there. It isn't before the first save, or after the player deleted it to start over. */
    public static boolean exists()
    {
        return !Files.notExists(FILE);
    }

    /**
     * The saved collection: the starter cards plus the cards unlocked in the file. A missing file is created, with no
     * cards in it; a file that can't be read counts as one with no cards.
     */
    public static CardCollection load(CardDatabase db)
    {
        Set<String> ids = new HashSet<>();
        if (Files.notExists(FILE))
        {
            CardCollection empty = CardCollection.of(db, ids);
            save(empty);
            return empty;
        }
        try
        {
            // broken bytes become replacement characters instead of failing the read, so the good lines are kept
            String text = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
            for (String line : text.split("\\R"))
            {
                String id = line.replace(BOM, "").trim();
                if (!id.isEmpty() && !id.startsWith("#"))
                {
                    ids.add(id);
                }
            }
        }
        catch (IOException e)
        {
            // can't be read right now (in use, no permission): counts as no cards
        }
        return CardCollection.of(db, ids);
    }

    /**
     * Saves the unlocked cards. The new file is written next to the old one, flushed to the disk and then moved over it
     * in one step, so the file is never left half written. If any step fails, the old file stays as it was.
     */
    public static void save(CardCollection collection)
    {
        List<String> lines = new ArrayList<>();
        for (String line : HEADER)
        {
            lines.add(line);
        }
        lines.addAll(collection.unlockedIds());
        try
        {
            Files.createDirectories(FILE.getParent());
            Files.write(TEMP, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.SYNC);
            try
            {
                Files.move(TEMP, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException e)
            {
                Files.move(TEMP, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e)
        {
            deleteTemp();
        }
    }

    /** Removes what a failed save left behind. */
    private static void deleteTemp()
    {
        try
        {
            Files.deleteIfExists(TEMP);
        }
        catch (IOException e)
        {
            // the next save writes over it
        }
    }
}
