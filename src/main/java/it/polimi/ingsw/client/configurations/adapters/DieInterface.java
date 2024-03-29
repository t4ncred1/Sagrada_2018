package it.polimi.ingsw.client.configurations.adapters;

import it.polimi.ingsw.client.configurations.AdapterInterface;
import it.polimi.ingsw.client.custom_exception.invalid_operations.DieNotExistException;
import it.polimi.ingsw.server.model.components.Die;

public abstract class DieInterface implements AdapterInterface {
    private Die die;

    /**
     * Constructor for DieInterface.
     *
     * @param die The die selected.
     * @throws DieNotExistException Thrown if the die does not exist.
     */
    public DieInterface(Die die) throws DieNotExistException {
        if(die==null) throw new DieNotExistException();
        this.die=die;
    }

    protected Die getDie(){
        return this.die;
    }
}
