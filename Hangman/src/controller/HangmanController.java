package controller;

import apptemplate.AppTemplate;
import data.GameData;
import gui.Workspace;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.shape.Rectangle;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import propertymanager.PropertyManager;
import ui.AppMessageDialogSingleton;
import ui.YesNoCancelDialogSingleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static settings.AppPropertyType.*;
import static settings.InitializationParameters.APP_WORKDIR_PATH;

/**
 * @author Andy Lau, Ritwik Banerjee
 */
public class HangmanController implements FileController {

    public enum GameState {
        UNINITIALIZED,
        INITIALIZED_UNMODIFIED,
        INITIALIZED_MODIFIED,
        ENDED
    }

    private AppTemplate appTemplate; // shared reference to the application
    private GameData    gamedata;    // shared reference to the game being played, loaded or saved
    private GameState   gamestate;   // the state of the game being shown in the workspace
    private Text[]      progress;    // reference to the text area for the word
    private Text[]      alldaguesses;
    private boolean     success;     // whether or not player was successful
    private int         discovered;  // the number of letters already discovered
    private Button      gameButton;  // shared reference to the "start game" button
    private Button      giveHint;
    private Label       remains;     // dynamically updated label that indicates the number of remaining guesses
    private Path        workFile;
    private ObservableList<Node> hangmanImage;
    private char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private ArrayList<Rectangle> rectangles = new ArrayList<Rectangle>();

    public HangmanController(AppTemplate appTemplate, Button gameButton, Button giveHint) {
        this(appTemplate);
        this.gameButton = gameButton;
        this.giveHint = giveHint;
    }

    public HangmanController(AppTemplate appTemplate) {
        this.appTemplate = appTemplate;
        this.gamestate = GameState.UNINITIALIZED;
    }

    public void enableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(false);
    }

    public void disableGameButton() {
        if (gameButton == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameButton = workspace.getStartGame();
        }
        gameButton.setDisable(true);
    }

    public void enableHintButton() {
        if (giveHint == null) {
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            giveHint  = workspace.getGiveHint();
        }
        giveHint.setDisable(false);

    }
    public void disableHintButton() {
        if (giveHint == null){
            Workspace workspace = (Workspace) appTemplate.getWorkspaceComponent();
            giveHint  = workspace.getGiveHint();
        }
        giveHint.setDisable(true);
    }

    public void setGameState(GameState gamestate) {
        this.gamestate = gamestate;
    }

    public GameState getGamestate() {
        return this.gamestate;
    }

    /**
     * In the homework code given to you, we had the line
     * gamedata = new GameData(appTemplate, true);
     * This meant that the 'gamedata' variable had access to the app, but the data component of the app was still
     * the empty game data! What we need is to change this so that our 'gamedata' refers to the data component of
     * the app, instead of being a new object of type GameData. There are several ways of doing this. One of which
     * is to write (and use) the GameData#init() method.
     */
    public void start() {
        gamedata = (GameData) appTemplate.getDataComponent();
        success = false;
        discovered = 0;

        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();


        gamedata.init();
        setGameState(GameState.INITIALIZED_UNMODIFIED);
        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        HBox guessedLetters    = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);
        HBox allGuesses        = gameWorkspace.getAllGuesses();
        if (gamedata.checkNeedHint(gamedata.getTargetWord())) {
            giveHint = gameWorkspace.getGiveHint();
            giveHint.setVisible(true);
        }
        remains = new Label(Integer.toString(GameData.TOTAL_NUMBER_OF_GUESSES_ALLOWED));
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);
        allGuesses.getChildren().addAll(new Label("Letters Guessed (Appear in Red): "));
        initWordGraphics(guessedLetters, allGuesses);
        play();
    }

    private void end() {
        appTemplate.getGUI().getPrimaryScene().setOnKeyTyped(null);
        gameButton.setDisable(true);
        giveHint.setDisable(true);
        setGameState(GameState.ENDED);
        appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
        Platform.runLater(() -> {
            PropertyManager           manager    = PropertyManager.getManager();
            AppMessageDialogSingleton dialog     = AppMessageDialogSingleton.getSingleton();
            String                    endMessage = manager.getPropertyValue(success ? GAME_WON_MESSAGE : GAME_LOST_MESSAGE);
            if (!success)
                endMessage += String.format(" :(");
            if (dialog.isShowing())
                dialog.toFront();
            else
                dialog.show(manager.getPropertyValue(GAME_OVER_TITLE), endMessage);
        });
    }

    private void initWordGraphics(HBox guessedLetters, HBox allGuesses) {
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        guessedLetters = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        for (int i = 0; i < targetword.length; i++) {
            StackPane stackPane = new StackPane();
            Rectangle rect = new Rectangle();
            rect.setWidth(30);
            rect.setHeight(30);
            rect.setFill(Color.BEIGE);
            rect.setStroke(Color.BLACK);
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(false);
            stackPane.getChildren().addAll(rect, progress[i]);
            guessedLetters.getChildren().add(stackPane);
            rectangles.add(i, rect);
        }

        alldaguesses = new Text[alphabet.length];
        for (int i = 0; i < alphabet.length; i++){
            alldaguesses[i] = new Text(Character.toString(alphabet[i]));
            alldaguesses[i].setVisible(true);
            alldaguesses[i].setOpacity(0.2);
        }
        allGuesses.getChildren().addAll(alldaguesses);
    }

    public void play() {
        System.out.print(gamedata.getTargetWord());
        disableGameButton();
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                appTemplate.getGUI().updateWorkspaceToolbar(gamestate.equals(GameState.INITIALIZED_MODIFIED));
                appTemplate.getGUI().getPrimaryScene().setOnKeyTyped((KeyEvent event) -> {
                    char guess = event.getCharacter().toLowerCase().charAt(0);
                    if (!alreadyGuessed(guess) && Character.isLetter(guess)) {
                        gamedata.addAllGuesses(guess);
                        for (int i = 0; i < alphabet.length; i++){
                            if (alphabet[i] == guess){
                                alldaguesses[i].setStroke(Color.RED);
                                alldaguesses[i].setOpacity(5);
                            }
                        }
                        boolean goodguess = false;
                        for (int i = 0; i < progress.length; i++) {
                            if (gamedata.getTargetWord().charAt(i) == guess) {
                                progress[i].setVisible(true);
                                gamedata.addGoodGuess(guess);
                                goodguess = true;
                                discovered++;
                            }
                        }
                        if (!goodguess) {
                            gamedata.addBadGuess(guess);
                            Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
                            hangmanImage = gameWorkspace.getHangmanImage();
                            for(int i = 0; i < gamedata.getBadGuesses().size(); i++)
                                hangmanImage.get(i).setVisible(true);
                        }

                        success = (discovered == progress.length);
                        remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
                    }
                    setGameState(GameState.INITIALIZED_MODIFIED);
                });
                if (gamedata.getRemainingGuesses() == 1)
                    disableHintButton();
                if (gamedata.getRemainingGuesses() <= 0 || success) {
                    for (int i = 0; i < progress.length; i++){
                        if (!progress[i].isVisible()) {
                            progress[i].setStroke(Color.BLUE);
                            progress[i].setVisible(true);
                            rectangles.get(i).setFill(Color.ORANGE);
                        }
                    }
                    stop();
                }
            }

            @Override
            public void stop() {
                super.stop();
                end();
            }
        };
        timer.start();
    }

    private void restoreGUI() {
        disableGameButton();
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        gameWorkspace.reinitialize();

        HBox guessedLetters = (HBox) gameWorkspace.getGameTextsPane().getChildren().get(1);
        HBox  allGuesses    =  gameWorkspace.getAllGuesses();
        allGuesses.getChildren().addAll(new Label("Letters Guessed (Appear in Red): "));
        restoreWordGraphics(guessedLetters, allGuesses);

        HBox remainingGuessBox = gameWorkspace.getRemainingGuessBox();
        remains = new Label(Integer.toString(gamedata.getRemainingGuesses()));
        remainingGuessBox.getChildren().addAll(new Label("Remaining Guesses: "), remains);

        giveHint = gameWorkspace.getGiveHint();
        if (gamedata.checkNeedHint(gamedata.getTargetWord()) && !gamedata.isHintUsed()) {
            enableHintButton();
            giveHint.setVisible(true);
        }

        if (gamedata.checkNeedHint(gamedata.getTargetWord()) && gamedata.isHintUsed()) {
            disableHintButton();
            giveHint.setVisible(true);
        }


        hangmanImage = gameWorkspace.getHangmanImage();
        for(int i = 0; i < gamedata.getBadGuesses().size(); i++)
            hangmanImage.get(i).setVisible(true);

        success = false;
        play();
    }

    private void restoreWordGraphics(HBox guessedLetters, HBox allGuesses) {
        Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
        discovered = 0;
        char[] targetword = gamedata.getTargetWord().toCharArray();
        progress = new Text[targetword.length];
        for (int i = 0; i < targetword.length; i++) {
            StackPane stackPane = new StackPane();
            javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle();
            rect.setWidth(30);
            rect.setHeight(30);
            rect.setFill(Color.BEIGE);
            rect.setStroke(Color.BLACK);
            progress[i] = new Text(Character.toString(targetword[i]));
            progress[i].setVisible(false);
            stackPane.getChildren().addAll(rect, progress[i]);
            guessedLetters.getChildren().add(stackPane);
            rectangles.add(i, rect);
        }
        for (int i = 0; i < progress.length; i++) {
            progress[i].setVisible(gamedata.getGoodGuesses().contains(progress[i].getText().charAt(0)));
            if (progress[i].isVisible())
                discovered++;
        }

        allGuesses        = gameWorkspace.getAllGuesses();
        alldaguesses = new Text[alphabet.length];
        for (int i = 0; i < alphabet.length; i++){
            alldaguesses[i] = new Text(Character.toString(alphabet[i]));
            alldaguesses[i].setVisible(true);
        }
        allGuesses.getChildren().addAll(alldaguesses);
        for (int i = 0; i < alphabet.length; i++){
            if (gamedata.getAllGuesses().contains(alphabet[i])){
                alldaguesses[i].setStroke(Color.RED);
                alldaguesses[i].setOpacity(5);
            }
        }
    }

    private boolean alreadyGuessed(char c) {
        return gamedata.getGoodGuesses().contains(c) || gamedata.getBadGuesses().contains(c);
    }

    @Override
    public void handleNewRequest() {
        AppMessageDialogSingleton messageDialog   = AppMessageDialogSingleton.getSingleton();
        PropertyManager           propertyManager = PropertyManager.getManager();
        boolean                   makenew         = true;
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            try {
                makenew = promptToSave();
            } catch (IOException e) {
                messageDialog.show(propertyManager.getPropertyValue(NEW_ERROR_TITLE), propertyManager.getPropertyValue(NEW_ERROR_MESSAGE));
            }
        if (makenew) {
            appTemplate.getDataComponent().reset();                // reset the data (should be reflected in GUI)
            appTemplate.getWorkspaceComponent().reloadWorkspace(); // load data into workspace
            ensureActivatedWorkspace();                            // ensure workspace is activated
            workFile = null;                                       // new workspace has never been saved to a file
            ((Workspace) appTemplate.getWorkspaceComponent()).reinitialize();

            enableGameButton();
            enableHintButton();
        }
        if (gamestate.equals(GameState.ENDED)) {
            appTemplate.getGUI().updateWorkspaceToolbar(false);
            Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
            gameWorkspace.reinitialize();
        }

    }

    @Override
    public void handleSaveRequest() throws IOException {
        PropertyManager propertyManager = PropertyManager.getManager();
        if (workFile == null) {
            FileChooser filechooser = new FileChooser();
            Path        appDirPath  = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path        targetPath  = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(SAVE_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension   = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                    String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showSaveDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null)
                save(selectedFile.toPath());
        } else
            save(workFile);
    }

    public void handleHintRequest() {
        for (int i = 0; i < gamedata.getTargetWord().length(); i++) {
                if (!progress[i].isVisible()) {
                    progress[i].setVisible(true);
                    gamedata.addGoodGuess(gamedata.getTargetWord().charAt(i));
                    gamedata.addBadGuess(gamedata.getTargetWord().charAt(i));
                    gamedata.addAllGuesses(gamedata.getTargetWord().charAt(i));
                    discovered++;
                    Workspace gameWorkspace = (Workspace) appTemplate.getWorkspaceComponent();
                    hangmanImage = gameWorkspace.getHangmanImage();
                    for (int x = 0; x < gamedata.getBadGuesses().size(); x++)
                        hangmanImage.get(x).setVisible(true);
                    for (int y = 0; y < alphabet.length; y++){
                        if (gamedata.getAllGuesses().contains(alphabet[y])){
                            alldaguesses[y].setStroke(Color.RED);
                            alldaguesses[y].setOpacity(5);
                        }
                    }
                    remains.setText(Integer.toString(gamedata.getRemainingGuesses()));
                    break;
                }
            }
        for (int i = 0; i <gamedata.getTargetWord().length(); i++) {
            if (!progress[i].isVisible() && gamedata.getGoodGuesses().contains(progress[i].getText().charAt(0))){
                progress[i].setVisible(true);
                discovered++;
            }
        }

        disableHintButton();
        gamedata.setIsHintUsed(true);

        if(discovered == gamedata.getTargetWord().length()) {
            success = true;
            end();
        }
    }

    @Override
    public void handleLoadRequest() throws IOException {
        boolean load = true;
        if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
            load = promptToSave();
        if (load) {
            PropertyManager propertyManager = PropertyManager.getManager();
            FileChooser     filechooser     = new FileChooser();
            Path            appDirPath      = Paths.get(propertyManager.getPropertyValue(APP_TITLE)).toAbsolutePath();
            Path            targetPath      = appDirPath.resolve(APP_WORKDIR_PATH.getParameter());
            filechooser.setInitialDirectory(targetPath.toFile());
            filechooser.setTitle(propertyManager.getPropertyValue(LOAD_WORK_TITLE));
            String description = propertyManager.getPropertyValue(WORK_FILE_EXT_DESC);
            String extension   = propertyManager.getPropertyValue(WORK_FILE_EXT);
            ExtensionFilter extFilter = new ExtensionFilter(String.format("%s (*.%s)", description, extension),
                    String.format("*.%s", extension));
            filechooser.getExtensionFilters().add(extFilter);
            File selectedFile = filechooser.showOpenDialog(appTemplate.getGUI().getWindow());
            if (selectedFile != null && selectedFile.exists())
                load(selectedFile.toPath());
            restoreGUI(); // restores the GUI to reflect the state in which the loaded game was last saved
        }
    }

    @Override
    public void handleExitRequest() {
        try {
            boolean exit = true;
            if (gamestate.equals(GameState.INITIALIZED_MODIFIED))
                exit = promptToSave();
            if (exit)
                System.exit(0);
        } catch (IOException ioe) {
            AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
            PropertyManager           props  = PropertyManager.getManager();
            dialog.show(props.getPropertyValue(SAVE_ERROR_TITLE), props.getPropertyValue(SAVE_ERROR_MESSAGE));
        }
    }

    private void ensureActivatedWorkspace() {
        appTemplate.getWorkspaceComponent().activateWorkspace(appTemplate.getGUI().getAppPane());
    }

    private boolean promptToSave() throws IOException {
        PropertyManager            propertyManager   = PropertyManager.getManager();
        YesNoCancelDialogSingleton yesNoCancelDialog = YesNoCancelDialogSingleton.getSingleton();

        yesNoCancelDialog.show(propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_TITLE),
                propertyManager.getPropertyValue(SAVE_UNSAVED_WORK_MESSAGE));

        if (yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.YES))
            handleSaveRequest();

        return !yesNoCancelDialog.getSelection().equals(YesNoCancelDialogSingleton.CANCEL);
    }

    /**
     * A helper method to save work. It saves the work, marks the current work file as saved, notifies the user, and
     * updates the appropriate controls in the user interface
     *
     * @param target The file to which the work will be saved.
     * @throws IOException
     */
    private void save(Path target) throws IOException {
        appTemplate.getFileComponent().saveData(appTemplate.getDataComponent(), target);
        workFile = target;
        setGameState(GameState.INITIALIZED_UNMODIFIED);
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager           props  = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(SAVE_COMPLETED_TITLE), props.getPropertyValue(SAVE_COMPLETED_MESSAGE));
    }

    /**
     * A helper method to load saved game data. It loads the game data, notified the user, and then updates the GUI to
     * reflect the correct state of the game.
     *
     * @param source The source data file from which the game is loaded.
     * @throws IOException
     */
    private void load(Path source) throws IOException {
        // load game data
        appTemplate.getFileComponent().loadData(appTemplate.getDataComponent(), source);

        // set the work file as the file from which the game was loaded
        workFile = source;

        // notify the user that load was successful
        AppMessageDialogSingleton dialog = AppMessageDialogSingleton.getSingleton();
        PropertyManager           props  = PropertyManager.getManager();
        dialog.show(props.getPropertyValue(LOAD_COMPLETED_TITLE), props.getPropertyValue(LOAD_COMPLETED_MESSAGE));

        setGameState(GameState.INITIALIZED_UNMODIFIED);
        Workspace gameworkspace = (Workspace) appTemplate.getWorkspaceComponent();
        ensureActivatedWorkspace();
        gameworkspace.reinitialize();
        gamedata = (GameData) appTemplate.getDataComponent();
    }
}
