package it.polimi.ingsw.client.configurations.adapters;

import it.polimi.ingsw.client.configurations.AdapterInterface;
import it.polimi.ingsw.server.model.components.Die;

import java.util.List;

public abstract class DicePoolInterface implements AdapterInterface {

    private List<Die> dicePool;

    /**
     * Constructor for DicePoolInterface.
     * @param dice The list of dice.
     */
    public DicePoolInterface(List<Die> dice){
        dicePool= dice;
    }

    protected List<Die> getDicePool(){
        return this.dicePool;
    }

    public Die getDie(int position){
        if(position<0||position>=dicePool.size()) throw new IndexOutOfBoundsException();
        return new Die(dicePool.get(position));
    }

    public int getDicePoolSize(){
        return dicePool.size();
    }

}
