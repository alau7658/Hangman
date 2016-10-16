package gui;

import apptemplate.AppTemplate;
import components.AppWorkspaceComponent;
import controller.HangmanController;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.paint.Color;
import propertymanager.PropertyManager;
import ui.AppGUI;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;

import static hangman.HangmanProperties.*;

/**
 * This class serves as the GUI component for the Hangman game.
 *
 * @author Ritwik Banerjee
 */
public class Workspace extends AppWorkspaceComponent {

    AppTemplate app; // the actual application
    AppGUI      gui; // the GUI inside which the application sits

    Label             guiHeadingLabel;   // workspace (GUI) heading label
    HBox              headPane;          // conatainer to display the heading
    HBox              bodyPane;          // container for the main game displays
    ToolBar           footToolbar;       // toolbar for game buttons
    BorderPane        figurePane;        // container to display the namesake graphic of the (potentially) hanging person
    VBox              gameTextsPane;     // container to display the text-related parts of the game
    HBox              guessedLetters;    // text area displaying all the letters guessed so far
    HBox              allGuesses;
    HBox              remainingGuessBox; // container to display the number of remaining guesses
    Button            startGame;         // the button to start playing a game of Hangman
    HangmanController controller;
    ObservableList<Node> hangmanImage;

    /**
     * Constructor for initializing the workspace, note that this constructor
     * will fully setup the workspace user interface for use.
     *
     * @param initApp The application this workspace is part of.
     * @throws IOException Thrown should there be an error loading application
     *                     data for setting up the user interface.
     */
    public Workspace(AppTemplate initApp) throws IOException {
        app = initApp;
        gui = app.getGUI();
        controller = (HangmanController) gui.getFileController();    //new HangmanController(app, startGame); <-- THIS WAS A MAJOR BUG!??
        layoutGUI();     // initialize all the workspace (GUI) components including the containers and their layout
        setupHandlers(); // ... and set up event handling
    }

    private void layoutGUI() {
        PropertyManager propertyManager = PropertyManager.getManager();
        guiHeadingLabel = new Label(propertyManager.getPropertyValue(WORKSPACE_HEADING_LABEL));

        headPane = new HBox();
        headPane.getChildren().add(guiHeadingLabel);
        headPane.setAlignment(Pos.CENTER);

        figurePane = new BorderPane();
        guessedLetters = new HBox();
        guessedLetters.setStyle("-fx-background-color: transparent;");
        remainingGuessBox = new HBox();
        allGuesses = new HBox();
        gameTextsPane = new VBox();

        gameTextsPane.getChildren().setAll(remainingGuessBox, guessedLetters, allGuesses);

        bodyPane = new HBox();
        bodyPane.getChildren().addAll(figurePane, gameTextsPane);

        startGame = new Button("Start Playing");
        HBox blankBoxLeft  = new HBox();
        HBox blankBoxRight = new HBox();
        HBox.setHgrow(blankBoxLeft, Priority.ALWAYS);
        HBox.setHgrow(blankBoxRight, Priority.ALWAYS);
        footToolbar = new ToolBar(blankBoxLeft, startGame, blankBoxRight);
        GridPane hangman = new GridPane();
        Label label1 = new Label("This is where the hangman will be located");
        ArrayList hangmanSpace = new ArrayList<Rectangle>();
        VBox red = new VBox();
        red.getChildren().addAll(hangmanSpace);
        hangman.getChildren().addAll(red, label1);
        Pane pane = new Pane();
        hangmanImage = pane.getChildren();
        initHanger();
        initHangman();


        workspace = new VBox();
        workspace.getChildren().addAll(headPane, bodyPane, pane, footToolbar);
    }

    public void initHangman(){
        ArrayList hangman = new ArrayList<Shape>();

        Circle head = new Circle(200,112,35);
        head.setStroke(Color.BLACK);
        head.setFill(Color.WHITE);
        head.setStrokeWidth(5);
        head.setVisible(false);
        hangmanImage.add(head);
        hangman.add(head);

        Line body = new Line(200,200,200,150);
        body.setStroke(Color.BLACK);
        body.setStrokeWidth(5);
        body.setVisible(false);
        hangmanImage.add(body);
        hangman.add(body);

        Line leftArm = new Line(150,225,200,175);
        leftArm.setStroke(Color.BLACK);
        leftArm.setStrokeWidth(5);
        leftArm.setVisible(false);
        hangmanImage.add(leftArm);
        hangman.add(leftArm);

        Line rightArm = new Line(250,225,200,175);
        rightArm.setStroke(Color.BLACK);
        rightArm.setStrokeWidth(5);
        rightArm.setVisible(false);
        hangmanImage.add(rightArm);
        hangman.add(rightArm);

        Line leftLeg = new Line(200,200,175,275);
        leftLeg.setStroke(Color.BLACK);
        leftLeg.setStrokeWidth(5);
        leftLeg.setVisible(false);
        hangmanImage.add(leftLeg);
        hangman.add(leftLeg);

        Line rightLeg = new Line(200,200,225,275);
        rightLeg.setStroke(Color.BLACK);
        rightLeg.setStrokeWidth(5);
        rightLeg.setVisible(false);
        hangmanImage.add(rightLeg);
        hangman.add(rightLeg);

    }

    public void initHanger(){
        Rectangle hangerBottom = new Rectangle(25, 25, 200, 25);
        hangerBottom.setStroke(Color.BROWN);
        hangerBottom.setStrokeWidth(3);
        hangerBottom.setVisible(false);
        hangmanImage.add(hangerBottom);

        Rectangle hangerStand = new Rectangle(25, 25, 25, 300);
        hangerStand.setStroke(Color.BROWN);
        hangerStand.setStrokeWidth(3);
        hangerStand.setVisible(false);
        hangmanImage.add(hangerStand);

        Rectangle hangerTop = new Rectangle(25, 300, 200, 25);
        hangerTop.setStroke(Color.BROWN);
        hangerTop.setStrokeWidth(3);
        hangerTop.setVisible(false);
        hangmanImage.add(hangerTop);

        Line hangerHook = new Line(200, 25, 200, 75);
        hangerHook.setStroke(Color.BROWN);
        hangerHook.setStrokeWidth(3);
        hangerHook.setVisible(false);
        hangmanImage.add(hangerHook);
    }

    private void setupHandlers() {
        startGame.setOnMouseClicked(e -> controller.start());
    }

    /**
     * This function specifies the CSS for all the UI components known at the time the workspace is initially
     * constructed. Components added and/or removed dynamically as the application runs need to be set up separately.
     */
    @Override
    public void initStyle() {
        PropertyManager propertyManager = PropertyManager.getManager();

        gui.getAppPane().setId(propertyManager.getPropertyValue(ROOT_BORDERPANE_ID));
        gui.getToolbarPane().getStyleClass().setAll(propertyManager.getPropertyValue(SEGMENTED_BUTTON_BAR));
        gui.getToolbarPane().setId(propertyManager.getPropertyValue(TOP_TOOLBAR_ID));

        ObservableList<Node> toolbarChildren = gui.getToolbarPane().getChildren();
        toolbarChildren.get(0).getStyleClass().add(propertyManager.getPropertyValue(FIRST_TOOLBAR_BUTTON));
        toolbarChildren.get(toolbarChildren.size() - 1).getStyleClass().add(propertyManager.getPropertyValue(LAST_TOOLBAR_BUTTON));

        workspace.getStyleClass().add(CLASS_BORDERED_PANE);
        guiHeadingLabel.getStyleClass().setAll(propertyManager.getPropertyValue(HEADING_LABEL));

    }

    /** This function reloads the entire workspace */
    @Override
    public void reloadWorkspace() {
        /* does nothing; use reinitialize() instead */
    }

    public VBox getGameTextsPane() {
        return gameTextsPane;
    }

    public HBox getAllGuesses(){
        return allGuesses;
    }

    public ObservableList<Node> getHangmanImage(){ return hangmanImage;}

    public HBox getRemainingGuessBox() {
        return remainingGuessBox;
    }

    public Button getStartGame() {
        return startGame;
    }

    public void reinitialize() {
        guessedLetters = new HBox();
        guessedLetters.setStyle("-fx-background-color: transparent;");
        remainingGuessBox = new HBox();
        allGuesses = new HBox();
        gameTextsPane = new VBox();
        gameTextsPane.getChildren().setAll(remainingGuessBox, guessedLetters, allGuesses);
        bodyPane.getChildren().setAll(figurePane, gameTextsPane);
        for(int i = 0; i < hangmanImage.size(); i++)
            hangmanImage.get(i).setVisible(false);
    }
}