package it.polimi.ingsw.server.model.cards;
import it.polimi.ingsw.server.model.components.Box;
import it.polimi.ingsw.server.model.components.Die;
import it.polimi.ingsw.server.model.components.DieConstraints;
import it.polimi.ingsw.server.model.components.Grid;
import it.polimi.ingsw.server.custom_exception.NotValidParameterException;

public class PrivateObjective extends Objective {
    private String color;
    private final static String TYPE = "private";

    /**
     * Constructor for PrivateObjInterface.
     *
     * @param color The color of the private objective card.
     * @throws NotValidParameterException Thrown when 'color' is not one of the 5 admitted ones.
     */
    public PrivateObjective(String color)throws NotValidParameterException {
        final String expectedColorType= "Color: red, yellow, green, blue, purple";
        if(color.equals("red")||color.equals("green")||color.equals("yellow")||color.equals("blue")||color.equals("purple"))
        this.color=color.toLowerCase();
        else throw new NotValidParameterException(color,expectedColorType);
        this.title="Obiettivo Privato";
        this.description="Guadagni un punto in piú per ogni dado inserito di colore: ";
    }

    /**
     * Constructor for PrivateObjInterface.
     *
     * @param privateObjective is an existing private Object
     */
    public PrivateObjective(PrivateObjective privateObjective) {
        color=privateObjective.getColor();
        description=privateObjective.getDescription();
        title=privateObjective.getTitle();
    }

    /**
     *
     * @return The type of the objective.
     */
    public String getType(){
        return TYPE;
    }

    /**
     *
     * @return The color of the objective.
     */
    public String getColor(){
        return color;
    }

    /**
     *
     * @return Color of the private objective.
     */
    public String showPrivateObjective(){
        return this.color;
    }

    @Override
    public int calculatePoints(Grid grid) {
        int points_to_add, i, j;
        String color_to_check;
        DieConstraints die_temp = null;
        Die die_temp1 = null;
        int return_value = 0;
        Box[][] actual_grid = grid.getGrid();

        for (i = 0; i < actual_grid.length; i++) {
            for (j = 0; j < actual_grid[i].length; j++) {

                try {
                    die_temp = actual_grid[i][j].getDieConstraint();
                    die_temp1 = die_temp.getDie();
                } catch (NullPointerException e) {
                    continue;
                }
                color_to_check = die_temp1.getColor();
                if (color_to_check.equals(color)) {
                    points_to_add = die_temp1.getValue();
                    return_value = return_value + points_to_add;
                }
            }
        }
        return return_value;
    }
}
