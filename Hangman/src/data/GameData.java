package data;

import apptemplate.AppTemplate;
import components.AppDataComponent;
import controller.GameError;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Ritwik Banerjee
 */
public class GameData implements AppDataComponent {

    public static final  int TOTAL_NUMBER_OF_GUESSES_ALLOWED = 10;
    private static final int TOTAL_NUMBER_OF_STORED_WORDS    = 330622;

    private String         targetWord;
    private Set<Character> goodGuesses;
    private Set<Character> badGuesses;
    private int            remainingGuesses;
    public  AppTemplate    appTemplate;

    public GameData(AppTemplate appTemplate) {
        this(appTemplate, false);
    }

    public GameData(AppTemplate appTemplate, boolean initiateGame) {
        if (initiateGame) {
            this.appTemplate = appTemplate;
            this.targetWord = setTargetWord();
            this.goodGuesses = new HashSet<>();
            this.badGuesses = new HashSet<>();
            this.remainingGuesses = TOTAL_NUMBER_OF_GUESSES_ALLOWED;
        } else {
            this.appTemplate = appTemplate;
        }
    }

    public void init() {
        this.targetWord = setTargetWord();
        this.goodGuesses = new HashSet<>();
        this.badGuesses = new HashSet<>();
        this.remainingGuesses = TOTAL_NUMBER_OF_GUESSES_ALLOWED;
        if (checkNeedHint(targetWord))
            System.out.print("lol");
    }

    @Override
    public void reset() {
        this.targetWord = null;
        this.goodGuesses = new HashSet<>();
        this.badGuesses = new HashSet<>();
        this.remainingGuesses = TOTAL_NUMBER_OF_GUESSES_ALLOWED;
        appTemplate.getWorkspaceComponent().reloadWorkspace();
    }

    public String getTargetWord() {
        return targetWord;
    }

    private String setTargetWord() {
        URL wordsResource = getClass().getClassLoader().getResource("words/words.txt");
        assert wordsResource != null;

        try(BufferedReader reader = new BufferedReader(new FileReader(wordsResource.getPath()))){
            String line = reader.readLine();
            List<String> lines = new ArrayList<String>();
            while (line!= null){
                lines.add(line);
                line = reader.readLine();
            }
            boolean flag = true;
            while(flag) {
                Random r = new Random();
                String randomWord = lines.get(r.nextInt(lines.size()));
                if (checkValidWord(randomWord)) {
                    return randomWord;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        throw new GameError("Unable to load initial target word.");
    }

    public boolean checkValidWord(String word) {
        char[] chars = word.toCharArray();

        for (char c : chars) {
            if (!Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    public boolean checkNeedHint(String word){
        long uniqueChars = word.chars().distinct().count();
        if (uniqueChars > 7)
            return true;
        else
            return false;
    }

    public GameData setTargetWord(String targetWord) {
        this.targetWord = targetWord;
        return this;
    }

    public Set<Character> getGoodGuesses() {
        return goodGuesses;
    }

    public GameData setGoodGuesses(Set<Character> goodGuesses) {
        this.goodGuesses = goodGuesses;
        return this;
    }

    public Set<Character> getBadGuesses() {
        return badGuesses;
    }

    public GameData setBadGuesses(Set<Character> badGuesses) {
        this.badGuesses = badGuesses;
        return this;
    }

    public int getRemainingGuesses() {
        return remainingGuesses;
    }

    public void addGoodGuess(char c) {
        goodGuesses.add(c);
    }

    public void addBadGuess(char c) {
        if (!badGuesses.contains(c)) {
            badGuesses.add(c);
            remainingGuesses--;
        }
    }


}