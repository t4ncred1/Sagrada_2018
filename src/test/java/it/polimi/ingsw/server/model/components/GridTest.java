package it.polimi.ingsw.server.model.components;

import it.polimi.ingsw.server.custom_exception.InvalidOperationException;
import it.polimi.ingsw.server.custom_exception.NotValidParameterException;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

public class GridTest {

    /*----------------------------------------------------------------------------------------------------*/

    //                                       Test sui costruttori

    /*----------------------------------------------------------------------------------------------------*/

    /*-**************************************************************************************************-*/
    //                                  Grid (int Difficulty, String Name)
    /*-**************************************************************************************************-*/

    @Test
    void gridCopy(){
        Grid test1 = null, test2 = null;
        try {
            test1=new Grid(3,"test");

            for (int i = 0; i < test1.getColumnNumber(); i++) {
                for (int j = 0; j < test1.getRowNumber(); j++) {
                    test1.createBoxInXY(i, j, "yellow");
                }
            }

        } catch (NotValidParameterException e) {
            fail("Failed Initialization");
        }

        test2=new Grid(test1);
        assertEquals(test1.toString(), test2.toString());



    }

    @Test
    void gridCopyModification(){
        Grid test1 = null, test2 = null;
        try {
            test1=new Grid(3,"test");

            for (int i = 0; i < test1.getColumnNumber(); i++) {
                for (int j = 0; j < test1.getRowNumber(); j++) {
                    test1.createBoxInXY(i, j, "none");
                }
            }

        } catch (NotValidParameterException e) {
            fail("Failed Initialization");
        }

        test1.initializeAllObservers();
        try {
            test1.insertDieInXY(0,0,true,true,new Die("yellow",2));
        } catch (NotValidParameterException | InvalidOperationException e) {
            fail("Test has failed. Modification of a copy of a grid also modifies the original grid.");
        }

        test2=new Grid(test1);
        test2.initializeAllObservers();
        assertEquals(test1.toString(), test2.toString());

        try {
            test2.insertDieInXY(0,1,true,true,new Die("red",6));
        } catch (NotValidParameterException e) {
            fail("Test has failed. Modification of a copy of a grid also modifies the original grid.");
        } catch (InvalidOperationException e){
            fail("Test has failed. Modification of a copy of a grid also modifies, wrongly, the original grid.");
        }

        assertNotEquals(test1.toString(), test2.toString());
    }



    @Test
    public void nullStringPassed() {
        //Given
        String passedName = null;
        int passedDifficulty = 3;

        //Assert
        assertThrows(NullPointerException.class, () -> {
            Grid testGrid = new Grid(passedDifficulty, passedName);
        });

    }

    @Test
    public void invalidDifficultyPassed() {

        //Given
        String passedName = "nomeGrid";
        int passedDifficulty = 1;

        //Assert
        Throwable exception = assertThrows(NotValidParameterException.class, () -> {
            Grid testGrid = new Grid(passedDifficulty, passedName);
        });
        assertEquals("Parameter: 1. Expected: Difficulty should have a value between 3 and 6 (both included)", exception.getMessage());

        //Given
        String passedName1 = "nomeGrid1";
        int passedDifficulty1 = 7;

        //Assert
        Throwable exception1 = assertThrows(NotValidParameterException.class, () -> {
            Grid testGrid1 = new Grid(passedDifficulty1, passedName1);
        });
        assertEquals("Parameter: 1. Expected: Difficulty should have a value between 3 and 6 (both included)", exception.getMessage());

    }

    @Test
    public void validParameterPassed() throws NotValidParameterException {
        //Given
        String passedName = "nomeGrid";
        int passedDifficulty = 3;

        //When
        Grid testGrid = new Grid(passedDifficulty, passedName);

        //Assert
        assertEquals(passedDifficulty, testGrid.getDifficulty());
        assertEquals(passedName, testGrid.getName());
    }



    /*----------------------------------------------------------------------------------------------------*/

    //                                       Test sui metodi

    /*----------------------------------------------------------------------------------------------------*/

    /*-**************************************************************************************************-*/
    //                         createBoxInXY(int x, int y, String constraint)
    /*-**************************************************************************************************-*/

    @Test
    public void nullConstraintPassed() throws NotValidParameterException {

        //Given
        String passedConstraint = null;
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        Grid toTest = new Grid(3, "name");

        //Assert
        assertThrows(NullPointerException.class, () -> toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint));
    }

    @Test
    public void createIndexesOutOfBound() throws NotValidParameterException {

        //Given
        String passedConstraint = "red";
        int passedCoordinateX = 10;
        int passedCoordinateY = 10;
        Grid toTest = new Grid(3, "name");

        //Assert
        Throwable exception = assertThrows(NotValidParameterException.class, () -> toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint));
        assertEquals("Parameter: (10,10). Expected: coordinates should be: 0<=x<=3 and 0<=y<=4", exception.getMessage());

    }

    @Test
    public void boxAlreadyCreated() throws NotValidParameterException {

        //Given
        String passedConstraint = "red";
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        Grid toTest = new Grid(3, "name");
        toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint); //box filled, can't create another one here

        //Assert
        Throwable exception = assertThrows(NotValidParameterException.class, () -> toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint));
        assertEquals("Parameter: (0,0). Expected: other coordinates: in this place already exist a Box!", exception.getMessage());
    }

    @Test
    public void invalidStringPassed() throws NotValidParameterException { //invalid color and invalid string are the same case


        //Given
        String passedConstraint = "invalidString";
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        Grid toTest = new Grid(3, "name");

        //Assert
        Throwable exception = assertThrows(NotValidParameterException.class, () -> toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint));
        assertEquals("Parameter: invalidString. Expected: Color: red, yellow, green, blue, purple", exception.getMessage());
    }

    @Test
    public void invalidValueConstraintPassed() throws NotValidParameterException {

        //Given
        String passedConstraint = "10";
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        Grid toTest = new Grid(3, "name");

        //Assert
        Throwable exception = assertThrows(NotValidParameterException.class, () -> toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint));
        assertEquals("Parameter: 10. Expected: Value: 1, 2, 3, 4, 5, 6", exception.getMessage());
    }

    @Test
    public void validValueConstraintPassed() throws NotValidParameterException {
        //Given
        String passedConstraint = "5";
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        Grid toTest = new Grid(3, "name");
        toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint);

        //Assert
        Throwable exception = assertThrows(NotValidParameterException.class, () -> toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint));
        assertEquals("Parameter: (0,0). Expected: other coordinates: in this place already exist a Box!", exception.getMessage());
    }

    @Test
    public void validColorConstraintPassed() throws NotValidParameterException {
        //Given
        String passedConstraint = "red";
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        Grid toTest = new Grid(3, "name");
        toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint);

        //Assert
        Throwable exception = assertThrows(NotValidParameterException.class, () -> toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint));
        assertEquals("Parameter: (0,0). Expected: other coordinates: in this place already exist a Box!", exception.getMessage());
    }

    @Test
    public void validNoneConstraintPassed() throws NotValidParameterException {
        //Given
        String passedConstraint = "none";
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        Grid toTest = new Grid(3, "name");

        //When
        toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint);

        //Assert
        Throwable exception = assertThrows(NotValidParameterException.class, () -> toTest.createBoxInXY(passedCoordinateX, passedCoordinateY, passedConstraint));
        assertEquals("Parameter: (0,0). Expected: other coordinates: in this place already exist a Box!", exception.getMessage());
    }

    @Test
    public void checkBoxOpened() throws NotValidParameterException, InvalidOperationException {
        //Given
        String passedConstraint = "none";
        int passedCoordinateX1 = 0;
        int passedCoordinateY1 = 0;
        int passedCoordinateX2 = 0;
        int passedCoordinateY2 = 3;
        Grid toTest1 = new Grid(3, "name");


        //When
        for (int i=0; i<toTest1.getColumnNumber(); i++){
            for (int j=0; j<toTest1.getRowNumber(); j++){
                toTest1.createBoxInXY(i,j,passedConstraint);
            }
        }

        //Assert
        //this box is opened, methods ends without throwing exceptions
        toTest1.insertDieInXY(passedCoordinateX1, passedCoordinateY1, true, true, new Die("red", 1));
        assertThrows(InvalidOperationException.class, () -> toTest1.insertDieInXY(passedCoordinateX2, passedCoordinateY2, true, true, new Die("red", 1)));
    }


    /*-**************************************************************************************************-*/
    //                         insertDieInXY(int x, int y, String constraint)
    /*-**************************************************************************************************-*/
    @Test
    public void nullDiePassed() throws NotValidParameterException {

        //Given
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        boolean colorCheck = true, valueCheck = true;
        Die passedDie = null;

        Grid toTest = new Grid(3, "name");
        //Assert
        assertThrows(NullPointerException.class, () -> toTest.insertDieInXY(passedCoordinateX, passedCoordinateY, colorCheck, valueCheck, passedDie));
    }

    @Test
    public void insertIndexesOutOfBound() throws NotValidParameterException {

        //Given
        int passedCoordinateX = 10;
        int passedCoordinateY = 10;
        boolean colorCheck = true, valueCheck = true;
        Die passedDie = new Die("red", 1);

        Grid toTest = new Grid(3, "name");

        //Assert
        Throwable exception = assertThrows(NotValidParameterException.class, () -> toTest.insertDieInXY(passedCoordinateX, passedCoordinateY, colorCheck, valueCheck, passedDie));
        assertEquals("Parameter: (10,10). Expected: coordinates should be: 0<=x<=3 and 0<=y<=4", exception.getMessage());
    }

    @Test
    public void insertDieAfterFirst() throws NotValidParameterException{
        int passedCoordinateX1 = 0;
        int passedCoordinateY1 = 0;
        int passedCoordinateX2 = 0;
        int passedCoordinateY2 = 2;

        boolean colorCheck= true, valueCheck=true;
        Die passedDie = new Die("red", 1);

        Grid toTest;
        toTest = new Grid(3,"name");

        for (int i=0; i<toTest.getColumnNumber(); i++){
            for ( int j =0; j< toTest.getRowNumber(); j++){
                try {
                    toTest.createBoxInXY(i,j,"none");
                } catch (NotValidParameterException e) {
                    fail("Test failed during initialization.");
                }
            }
        }

        toTest.initializeAllObservers();

        try {
            toTest.insertDieInXY(passedCoordinateX1,passedCoordinateY1,colorCheck,valueCheck,passedDie);
        } catch (InvalidOperationException e) {
            fail("test failed in initialization.");
        }

        assertThrows(InvalidOperationException.class, ()->{
            toTest.insertDieInXY(passedCoordinateX2,passedCoordinateY2,colorCheck,valueCheck,passedDie);
        });

    }

    @Test
    public void gridNotInitialized() throws NotValidParameterException {


        //Given
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        boolean colorCheck = true, valueCheck = true;
        Die passedDie = new Die("red", 1);

        Grid toTest = new Grid(3, "name");

        //Assert

        assertThrows(NotValidParameterException.class, () -> toTest.insertDieInXY(passedCoordinateX, passedCoordinateY, colorCheck, valueCheck, passedDie));
    }

    @Test
    public void tryToInsertDieReturnFalse() throws NotValidParameterException, InvalidOperationException {

        //Given
        int passedCoordinateX = 1;
        int passedCoordinateY = 1;
        boolean colorCheck = true, valueCheck = true;
        String passedConstraint = "none";
        Die passedDie = new Die("red", 1);

        Grid toTest = new Grid(3, "name");

        for (int i = 0; i < toTest.getColumnNumber(); i++) {
            for (int j = 0; j < toTest.getRowNumber(); j++) {
                toTest.createBoxInXY(i, j, "none");
            }
        }
        toTest.initializeAllObservers();

        //Assert
        assertThrows(InvalidOperationException.class, () -> toTest.insertDieInXY(passedCoordinateX, passedCoordinateY, colorCheck, valueCheck, passedDie));

    }

    @Test
    public void tryToInsertDieReturnTrue() throws NotValidParameterException, InvalidOperationException {

        //Given
        int passedCoordinateX = 0;
        int passedCoordinateY = 0;
        boolean colorCheck = true, valueCheck = true;
        String passedConstraint = "none";
        Die passedDie = new Die("red", 1);

        Grid toTest = new Grid(3, "name");
        for (int i = 0; i < toTest.getColumnNumber(); i++) {
            for (int j = 0; j < toTest.getRowNumber(); j++) {
                toTest.createBoxInXY(i, j, "none");
            }
        }
        toTest.initializeAllObservers();


        //When
        toTest.insertDieInXY(passedCoordinateX, passedCoordinateY, colorCheck, valueCheck, passedDie); //insert in a opened box

        //Assert
        assertThrows(InvalidOperationException.class, () -> toTest.insertDieInXY(passedCoordinateX, passedCoordinateY, colorCheck, valueCheck, passedDie)); //reinsert should throw an exception.
    }

    /*----------------------------------------------------------------------------------------------------*/
    //                                       METODO associateObserver
    /*----------------------------------------------------------------------------------------------------*/
    @Test
    public void gridIsNotinitialized() {
        Grid toTest = null;
        try {
            toTest = new Grid(3, "testGrid");
        } catch (NotValidParameterException e) {
            fail("test failed");
        }
        assertThrows(NullPointerException.class, toTest::initializeAllObservers);
    }

    @Test
    public void gridIsOk() {
        Grid toTest = null;
        try {
            toTest = new Grid(3, "testGrid");
            for (int i = 0; i < toTest.getColumnNumber(); i++) {
                for (int j = 0; j < toTest.getRowNumber(); j++)
                    toTest.createBoxInXY(i, j, "none");
            }
        } catch (NotValidParameterException e) {
            fail("test failed");
        }
        toTest.initializeAllObservers();
    }

    @Test
    public void gridIsNotComplete() {
        Grid toTest = null;
        try {
            toTest = new Grid(3, "testGrid");
            for (int i = 0; i < toTest.getColumnNumber(); i++) {
                for (int j = 0; j < toTest.getRowNumber() - 1; j++)
                    toTest.createBoxInXY(i, j, "none");
            }
        } catch (NotValidParameterException e) {
            fail("test failed");
        }
        assertThrows(NullPointerException.class, toTest::initializeAllObservers);
    }

    @Test
    public void checkToString() throws NotValidParameterException, NullPointerException {
        Grid toTest = null;
        int i, j;
        try {
            toTest = new Grid(4, "toTest");
            for (i = 0; i < toTest.getColumnNumber(); i++) {
                for (j = 0; j < toTest.getRowNumber(); j++) {
                    toTest.createBoxInXY(i, j, "none");
                }
            }
        } catch (NotValidParameterException e) {
            fail("test failed");
        }
        for(String[] strings : toTest.getStructure()){
            for(String s : strings){
                assertEquals(s," ");
            }
        }
    }

    @Test
    public void checkString() throws NotValidParameterException {
        Grid toTest=null;
        int i,j;
        try {
            toTest=new Grid(4,"test");
            for(i=0;i<toTest.getColumnNumber();i++){
                for(j=0;j<toTest.getRowNumber();j++){
                    toTest.createBoxInXY(i,j,"none");
                }
            }
        } catch(NotValidParameterException e){
            fail("test failed");
        }

        assertEquals("nome: test\tDifficoltà: 4\nWasFirstDieInserted: false\n" +
                "Boxes di test:\n" +
                " colonna 1:\n" +
                "\t riga 1: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (0,0)\n" +
                "\t riga 2: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (0,1)\n" +
                "\t riga 3: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (0,2)\n" +
                "\t riga 4: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (0,3)\n" +
                " colonna 2:\n" +
                "\t riga 1: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (1,0)\n" +
                "\t riga 2: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 0 | position = (1,1)\n" +
                "\t riga 3: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 0 | position = (1,2)\n" +
                "\t riga 4: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (1,3)\n" +
                " colonna 3:\n" +
                "\t riga 1: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (2,0)\n" +
                "\t riga 2: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 0 | position = (2,1)\n" +
                "\t riga 3: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 0 | position = (2,2)\n" +
                "\t riga 4: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (2,3)\n" +
                " colonna 4:\n" +
                "\t riga 1: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (3,0)\n" +
                "\t riga 2: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 0 | position = (3,1)\n" +
                "\t riga 3: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 0 | position = (3,2)\n" +
                "\t riga 4: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (3,3)\n" +
                " colonna 5:\n" +
                "\t riga 1: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (4,0)\n" +
                "\t riga 2: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (4,1)\n" +
                "\t riga 3: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (4,2)\n" +
                "\t riga 4: \n" +
                "\t\tcolor Restriction: 0; 0; 0; 0; 0; | value Restriction: 0; 0; 0; 0; 0; 0;  | Kind of constraint (T:value, F:color) false| Open = 1 | position = (4,3)\n",toTest.toString());
    }

    @Test
    public void checkRemoveDieFromXY() throws NotValidParameterException,InvalidOperationException {
        Die temp=new Die("green",1);
        int i,j;
        int x1=-1,x2=7;
        int y1=-1,y2=7;
        Grid toTest=null;
        try {
            toTest=new Grid(4,"test");
            for(i=0;i<toTest.getColumnNumber();i++){
                for(j=0;j<toTest.getRowNumber();j++){
                    toTest.createBoxInXY(i,j,"none");
                }
            }
        } catch(NotValidParameterException e){
            fail("test failed");
        }
        Grid finalToTest = toTest;
        assertThrows(NotValidParameterException.class,()-> finalToTest.removeDieFromXY(x1,0));
        assertThrows(NotValidParameterException.class,()->finalToTest.removeDieFromXY(x2,0));
        assertThrows(NotValidParameterException.class,()->finalToTest.removeDieFromXY(0,y1));
        assertThrows(NotValidParameterException.class,()->finalToTest.removeDieFromXY(0,y2));
        finalToTest.insertDieInXY(0,0,true,true,temp);
        assertThrows(InvalidOperationException.class,()->finalToTest.removeDieFromXY(1,1));
        finalToTest.removeDieFromXY(0,0);

    }

    @Test
    public void checkInitializeAllObservers(){
        Grid toTest=null;
        int i,j;
        try {
            toTest=new Grid(4,"test");
            for(i=0;i<toTest.getColumnNumber();i++){
                for(j=0;j<toTest.getRowNumber();j++){
                    toTest.createBoxInXY(i,j,"none");
                }
            }
        } catch(NotValidParameterException e){
            fail("test failed");
        }

        toTest.initializeAllObservers();
    }
}