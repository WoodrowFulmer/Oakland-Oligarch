package game;

import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.io.IOException;
import java.lang.Throwable;

/**
 * A class that will handle the saving and loading of the game.
 *
 * @author Woodrow Fulmer
 */
public class FileHandler{
	private static final String DEFAULT_FILE_NAME = "defaultFile.txt";
	
	private ArrayList<Player> playerList;	
	private Time time;
	private Square[] squareList;
	private int GO_PAYOUT;
	private int playerTurn;
	private int JAIL_POS;
	private int activePlayers;
	
	/**
	 * Creates a FileHandler to save and load from any files.
	 *
	 * @param	File	The file to load from
	 */
	public FileHandler(File f) {
		squareList = new Square[OaklandOligarchy.NUMBER_OF_TILES];
		playerList = new ArrayList<Player>(OaklandOligarchy.MAX_NUMBER_OF_PLAYERS);
		activePlayers = 0;
		load(f);
	}
	
	/**
	 * Creates a FileHandler to save a load from any files.
	 *
	 * @param	String	The name of the file to load
	 */
	public FileHandler(String fn) {
		this(new File(fn));
	}
	
	/**
	 * Creates a FileHandler to save and load from any files.
	 */
	public FileHandler() {
		this(DEFAULT_FILE_NAME);
	}
	
	public Time getTime() {return time;}
	public Square[] getBoard() {return squareList;}
	public Player[] getPlayerList() {return playerList.toArray(new Player[playerList.size()]);}
	public int getPayout() {return GO_PAYOUT;}
	public int getJailPosition() {return JAIL_POS;}
	public int getActivePlayers() {return activePlayers;}
	public int getPlayerTurn() {return playerTurn;}
	
	/**
	 * Reads through a given file and parses out all the information into appropriate structures
	 *
	 * @param	file	File to be loaded from
	 */
	private void load(File f) {
		int[] ownerList = new int[OaklandOligarchy.NUMBER_OF_TILES];
		try{
			Scanner reader = new Scanner(f);
			while (reader.hasNextLine()) {
				String[] input = reader.nextLine().split("\t+");
				if(input.length > 1) {
					loadLine(input, ownerList);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		setSquareOwners(ownerList);
		loadPlayersIntoJail();
	} 
	
	/**
	 * Loads all the appropriate information from a single line of text
	 *		which has been split at each tab.
	 *
	 * @param	input		A single line from the file, split by tabs
	 * @param	ownerList	A list which matches property indices to the ID of their owner
	 */
	private void loadLine(String[] input, int[] ownerList) {
		if(input[0].equals("Time")) {
			time = new Time(Integer.parseInt(input[1]));
		}
		else if(input[0].equals("GoPayout")) {
			GO_PAYOUT = Integer.parseInt(input[1]);
		}
		else if(input[0].equals("Player")) {
			loadPlayer(input);
		}
		else {
			loadSquare(input, ownerList);
		}
	}
	
	/**
	 * Loads player info from a line that is designated as a player.
	 *
	 * @param	input	A single line from the file, split by tabs
	 */
	private void loadPlayer(String[] input) {
		Player p = new Player(Integer.parseInt(input[1]), Integer.parseInt(input[4]), input[2]);
		p.setPosition(Integer.parseInt(input[5]));
		if(p.getMoney() < 0) {
			p.setLoser(true);
		}
		else {
			activePlayers++;
		}
		p.setColor(Integer.decode(input[3]));
		if(input[6].equals("*")){
			playerTurn = p.getId();
		}
		int jail = Integer.parseInt(input[7]);
		
		if(jail >= 0) {
			p.goToJail();
			for(int i = 0; i < jail; i++) {
				p.addToJailCounter();
			}
		}
		playerList.add(p);
	}
	
	/**
	 * Parses a single line from a file for information pertaining to a Square.
	 *
	 * @param	input		A single line from the file, split by tabs
	 * @param	ownerList	A list which matches property indices to the ID of their owner
	 */
	private void loadSquare(String[] input, int[] ownerList) {
		int current = Integer.parseInt(input[1]);
		if(input[0].equals("Property")) {
			squareList[current] = new Property(input[2], Integer.parseInt(input[3]), Integer.parseInt(input[4]));
			ownerList[current] = Integer.parseInt(input[5]);
			if(input[5].equals("m")) {
				((Property)squareList[current]).setMortgaged(true);
			}
		}
		else if(input[0].equals("Jail")) {
			JAIL_POS = current;
			squareList[current] = new JailSquare("Jail");
			ownerList[current] = -1;
		}
		else if(input[0].equals("Go")) {
			squareList[Integer.parseInt(input[1])] = new GoSquare();
			ownerList[current] = -1;
		}
	}
	
	/**
	 * Assigns unmarked squares as actions. Sets the owner of each property if it has one.
	 *
	 * @param	ownerList	A list which matches property indices to the ID of their owner
	 */
	private void setSquareOwners(int[] ownerList) {
		for (int i = 0; i < squareList.length; i++) {
			if (squareList[i] == null) {
				squareList[i] = new ActionSquare("Action");
			}
			else if(ownerList[i] > -1 && ownerList[i] < playerList.size()) {
				Player player = playerList.get(ownerList[i]);
				Property property = (Property)squareList[i];
				if(!player.getLoser()) {
					player.addProperty(property);
				}
			}
		}
	}
	
	/**
	 * Puts a player in jail if they are incarcerated.
	 */
	private void loadPlayersIntoJail() {
		for(Player p: playerList) {
			if(p.isInJail()) {
				((JailSquare)squareList[JAIL_POS]).addPrisoner(p);
			}
		}
	}
	
	/**
	 * Saves the time, the go payout, all square info, and all player info to a given file.
	 *
	 * @param	file		The file to save to
	 * @param	time		The elapsed game time
	 * @param	players		The full list of players
	 * @param	squares		All the squares on the board
	 * @param	playerTurn	The ID of the player whose turn it is
	 */
	public void save(File file, Time time, Player[] players, Square[] squares, int playerTurn) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Time\t" + time.getTime());
			bw.newLine();
			bw.write("GoPayout\t" + GO_PAYOUT);
			bw.newLine();
			for(Player p: players) {
				savePlayer(bw, p, playerTurn);
			}
			bw.newLine();
			for(int i = 0; i < squares.length; i++) {
				saveSquare(bw, squares[i], i);
			}
			bw.close();
		} catch (IOException except) {}
	}
	
	/**
	 * Writes all the information from a given player to the specified output buffer.
	 *
	 * @param	bw			The output buffer to write to
	 * @param	p			The player to be saved to file
	 * @param	playerTurn	The ID of the player whose turn it is
	 */
	private void savePlayer(BufferedWriter bw, Player p, int playerTurn) throws IOException {
		bw.write("Player\t");
		bw.write(p.getId()+"\t");
		bw.write(p.getName()+"\t");
		bw.write(p.getColor()+"\t");
		bw.write(p.getMoney() + "\t");
		bw.write(p.getPosition() + "\t");
		if(p.getId() == playerTurn) {
			bw.write("*\t");
		}
		else {
			bw.write("-\t");
		}
		if(p.isInJail()) {
			bw.write(p.getJailCounter());
		}
		else {
			bw.write("-1");
		}
		bw.newLine();
	}
	
	/**
	 * Writes all the information from a given square to the specified output buffer.
	 *
	 * @param	bw		The output buffer to write to
	 * @param	s		The square to be saved to file
	 * @param	index	The index of the given square within the board
	 */	
	private void saveSquare(BufferedWriter bw, Square s, int index) throws IOException {
		if(s instanceof Property) {
			saveProperty(bw, (Property)s, index);
		}
		else if(s instanceof JailSquare) {
			bw.write("Jail\t" + index);
			bw.newLine();
		}
		else if(s instanceof GoSquare) {
			bw.write("Go\t" + index);
			bw.newLine();
		}
	}
	
	/**
	 * Writes all the information from a given property to the specified output buffer.
	 *
	 * @param	bw		The output buffer to write to
	 * @param	p		The property to be saved to file
	 * @param	index	The index of the given property within the board
	 */	
	private void saveProperty(BufferedWriter bw, Property p, int index) throws IOException {
		bw.write("Property\t");
		bw.write(index + "\t");
		bw.write(p.getName() + "\t");
		bw.write(p.getPrice() + "\t");
		bw.write(p.getRent() + "\t");
		Player owner = p.getOwner();
		if(owner == null) {
			bw.write("-1\t");
		}
		else {
			bw.write(owner.getId() + "\t" );
		}
		if(p.getMortgaged()) {
			bw.write("m");
		}
		else {
			bw.write("u");
		}
		bw.newLine();
	}
}