package it.polimi.ingsw.server.model.cards.effects;

import it.polimi.ingsw.server.custom_exception.EffectException;
import it.polimi.ingsw.server.custom_exception.NotValidParameterException;
import it.polimi.ingsw.server.model.MatchModel;
import it.polimi.ingsw.server.model.cards.ToolCard;
import it.polimi.ingsw.server.model.components.Die;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChangeValueDiceEffect implements Effect {
    private transient MatchModel model;
    private transient ToolCard toolCard;
    private static final String NAME = "ChangeValueDiceEffect";


    @Override
    public void setParameters(MatchModel matchModel, ToolCard toolCard) {
        this.model = matchModel;
        this.toolCard = toolCard;
    }

    @Override
    public void executeTest() throws EffectException {
        if(toolCard.isMustBeSecondTurn() && model.getCurrentPlayer().isFirstTurn()) throw new EffectException("Must be player's second turn to activate this toolcard.");
        List<Die> dice = new ArrayList<>();
        for (Die d : toolCard.getDiceRemoved()){
            dice.add(new Die(d));
        }
        for(int i=0; i<dice.size();i++){
            Die die = dice.remove(i);
            try {
                dice.add(i, new Die(die.getColor(),new Random().nextInt(6) + 1));
            } catch (NotValidParameterException e) {
                throw new EffectException("Invalid value for die.");
            }
        }
        toolCard.saveDiceRemoved(dice); //set or save?
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void setToolCardParams(List<String> params) throws NotValidParameterException {
        if(!params.isEmpty()) throw new NotValidParameterException(params.toString(),"expected a void list");
    }

    @Override
    public void execute() {
        List<Die> dice = toolCard.getDiceRemoved();
        for(int i=0; i<dice.size();i++){
            Die die = dice.remove(i);
            try {
                dice.add(i, new Die(die.getColor(),new Random().nextInt(6) + 1));
            } catch (NotValidParameterException e) {
                Logger logger = Logger.getLogger(getClass().getName());
                logger.log(Level.WARNING, "Failed execution of effect \""+ NAME + "\" in toolcard "+toolCard.getTitle(), e);
            }
        }
        toolCard.setDiceRemoved(dice); //set because is executed on all dice removed.
    }



}
