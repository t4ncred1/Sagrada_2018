package it.polimi.ingsw.client.configurations.adapters;

import it.polimi.ingsw.server.model.cards.effects.Effect;

import java.util.List;

public abstract class EffectAdapter {
    Effect effect;

    public EffectAdapter(Effect effect){
        this.effect=effect;
    }

    public String getName(){
        return effect.getName();
    }

    public abstract List<String> computeEffect();
}
