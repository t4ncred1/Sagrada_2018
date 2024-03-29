package it.polimi.ingsw.server.model.cards.effects;

import it.polimi.ingsw.server.custom_exception.EffectException;
import it.polimi.ingsw.server.custom_exception.NotValidParameterException;
import it.polimi.ingsw.server.model.MatchModel;
import it.polimi.ingsw.server.model.cards.ToolCard;
import it.polimi.ingsw.server.model.components.Die;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IncrementDiceEffect implements Effect {
    private transient MatchModel model;
    private transient ToolCard toolCard;
    private static final String NAME = "IncrementDiceEffect";

    @Override
    public void setParameters(MatchModel matchModel, ToolCard toolCard){
        this.model=matchModel;
        this.toolCard=toolCard;
    }

    @Override
    public void executeTest() throws EffectException {
        ArrayList<Die>dice = new ArrayList<>();
        for (Die d : toolCard.getDiceRemoved()){
            dice.add(new Die(d));
        }
        for (int i=0; i<dice.size(); i++){
            if(toolCard.isIncrement() && dice.get(i).getValue()>=6) throw new EffectException("An increment effect was tried on a die that has already a value of 6 in toolcard "+toolCard.getTitle());
            else if (!toolCard.isIncrement() && dice.get(i).getValue() <=1) throw new EffectException("A decrement effect was tried on a die that has already a value of 1 in toolcard "+toolCard.getTitle());
            Die d = dice.remove(i);
            try {
                dice.add(i,new Die(d.getColor(), (toolCard.isIncrement() ? d.getValue() +1 : d.getValue()-1)));
            } catch (NotValidParameterException e) {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING,"Something went wrong in toolcard "+ toolCard.getTitle()+" with effect "+getName()+".");
                throw new EffectException("something went wrong!");
            }
        }
        toolCard.setDiceRemoved(dice);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void setToolCardParams(List<String> params) throws NotValidParameterException {
        final int REMOVING_INDEX=0;
        if(params.isEmpty()) throw new NotValidParameterException("An empty list","A not empty list");
        if(params.size()!=1) throw new NotValidParameterException(params.toString(),"Too many parameters");
        boolean increment= Boolean.parseBoolean(params.get(REMOVING_INDEX));
        if(!increment&&!params.get(REMOVING_INDEX).equalsIgnoreCase("false"))
            throw new NotValidParameterException(params.get(REMOVING_INDEX), "expected true or false");
        toolCard.setIncrement(increment);
    }

    @Override
    public void execute() {
        List<Die> dice = toolCard.getDiceRemoved();
        for (int i=0; i<dice.size(); i++){
            Die d = dice.remove(i);
            try {
                dice.add(i, new Die(d.getColor(), (toolCard.isIncrement() ? d.getValue() + 1 : d.getValue() - 1)));
            } catch (NotValidParameterException e) {
                Logger logger = Logger.getLogger(getClass().getName());
                logger.log(Level.WARNING, "Failed execution of effect \"" + NAME + "\" in toolcard " + toolCard.getTitle(), e);
            }
        }
        toolCard.setDiceRemoved(dice);
    }
}
