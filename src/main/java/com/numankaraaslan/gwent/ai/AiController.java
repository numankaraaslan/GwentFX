package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.ChoiceRequest;
import com.numankaraaslan.gwent.engine.GameAbortedException;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.Move;
import com.numankaraaslan.gwent.engine.PlayerController;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.Difficulty;
import com.numankaraaslan.gwent.model.RowType;
import java.util.List;
import java.util.Random;

/**
 * The computer opponent. Decisions come from {@link AiBrain}; this class adds short, human-like pauses so the player can
 * follow what happens. Runs on the engine thread; never modifies the game state.
 */
public final class AiController implements PlayerController
{

    private static final long MOVE_DELAY_MIN = 650;
    private static final int MOVE_DELAY_RANGE = 550;
    private static final long CHOICE_DELAY = 450;
    private static final long REDRAW_DELAY = 300;
    private static final long GO_FIRST_DELAY = 400;

    private final AiBrain brain;
    private final Random random = new Random();

    public AiController(Difficulty difficulty)
    {
        this.brain = new AiBrain(difficulty);
    }

    @Override
    public int chooseRedraw(GameState state, int player, int redrawsLeft)
    {
        long start = System.currentTimeMillis();
        int answer = brain.chooseRedraw(state, player, redrawsLeft);
        pauseUntil(start + REDRAW_DELAY);
        return answer;
    }

    /** Scoia'tael perk: let the human start, so the AI sees their first move. */
    @Override
    public boolean chooseGoFirst(GameState state, int player)
    {
        pauseUntil(System.currentTimeMillis() + GO_FIRST_DELAY);
        return false;
    }

    @Override
    public Move chooseMove(GameState state, int player, List<Move> legalMoves)
    {
        long start = System.currentTimeMillis();
        Move move = brain.chooseMove(state, player, legalMoves);
        pauseUntil(start + MOVE_DELAY_MIN + random.nextInt(MOVE_DELAY_RANGE));
        return legalMoves.contains(move) ? move : Move.pass();
    }

    @Override
    public List<Integer> chooseCards(GameState state, int player, ChoiceRequest request)
    {
        long start = System.currentTimeMillis();
        List<Integer> answer = brain.chooseCards(state, player, request);
        pauseUntil(start + CHOICE_DELAY);
        return answer;
    }

    @Override
    public RowType chooseRow(GameState state, int player, CardSnapshot card, List<RowType> options)
    {
        return brain.chooseRow(state, player, card, options);
    }

    @Override
    public void reveal(GameState state, int player, String title, List<CardSnapshot> cards)
    {
        brain.remember(cards);
    }

    /** Sleeps until the given time; an interrupt (game abandoned) ends the game thread. */
    private static void pauseUntil(long deadline)
    {
        long wait = deadline - System.currentTimeMillis();
        if (wait <= 0)
        {
            return;
        }
        try
        {
            Thread.sleep(wait);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new GameAbortedException(e);
        }
    }
}
