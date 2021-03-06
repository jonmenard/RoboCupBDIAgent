//
//	File:			Brain.java
//	Author:         Krzysztof Langner
//	Date:			1997/04/28
//
//  Modified by:	Paul Marlow

//  Modified by:    Edgar Acosta
//  Date:           March 4, 2008


//  Modefied By:    Hari Govindasamy,
//                  James Horner,
//                  Jon Menard,
//                  Micheal Marsland
//  Date:           2021/12/10


import java.lang.Math;
import java.util.*;
import java.util.regex.*;

import javax.sound.sampled.Line;


/**
*   Handler class for the BDIAgent
*   This class descritizes the envrionment into "Beliefs" and also
*   sends those beliefs to the JasonAgent to get a decision based on the agent's
*   desires as stored initally in the asl file for the agent
*   then an action is returned and executed by this handler
*/

class Brain extends Thread implements SensorInput
{

	private SendCommand	m_krislet;			// robot which is controled by this brain
    volatile private boolean m_timeOver;
    private String m_playMode;
    private String m_team;
	private Memory m_memory;				// place where all information is stored
    private char m_side;
	private String m_agent_asl;
    private List<Belief> perceptions;
    private ObjectInfo[] environmentObjects;


	//---------------------------------------------------------------------------
    // This constructor:
    // - stores connection to krislet
    // - starts thread for this object
    // - sets which ASL file should be used for the agent
    /**
     * @brief Constructor for Brain class used by Krislet.
     * @param krislet   Krislet interface to RoboCup server.
     * @param team      String name of the team the player is on.
     * @param side      Char l/r side the pitch of the agents team.
     * @param agent_asl String file name of the ASL file that the BDI agent should use.
     * @param playMode  String for the state of play in RoboCup.
     */
    public Brain(SendCommand krislet, String team, char side, String agent_asl, String playMode){
    	m_timeOver = false;
    	m_krislet = krislet;
    	m_memory = new Memory();
    	m_team = team;
    	m_side = side;
    	m_agent_asl = agent_asl;
    	m_playMode = playMode;
    	start();
        perceptions = new LinkedList<Belief>();
        environmentObjects = new ObjectInfo[7];
    }

    /**
     * @brief Operation getPerceptions determines what the player can currently see and
     * transforms the percepts into beliefs.
     * @details getPerceptions takes the current environment state percieved by the player
     * and stored in m_memory and returns a list of descritized perceptions that
     * the agent will have about the current environment.
     * @return List of Beliefs to add to the agents belief base.
     */
    public List<Belief> getPerceptions() {
        //Memory objects that will be used to identify if percepts are in the players memory.
        BallInfo ball = (BallInfo) m_memory.getObject("ball");

        GoalInfo ownGoal;
        GoalInfo opposingGoal;
        LineInfo ownSideLine;
        FlagInfo centre_c = (FlagInfo) m_memory.getObject("flag c");
        FlagInfo ownPenalty_c;

        /*
        if(centre_c != null){
            //System.out.println("Flag c: " + centre_c.getDistance());
        }
        */

        //Determine which side the player is on then initialise the memory objects accordingly.
        if(this.m_side == 'r'){
            ownGoal = (GoalInfo) m_memory.getObject("goal r");
            opposingGoal = (GoalInfo) m_memory.getObject("goal l");
            ownPenalty_c = (FlagInfo) m_memory.getObject("flag p r c");
            ownSideLine = (LineInfo) m_memory.getObject("line r");
        }else{
            ownGoal = (GoalInfo) m_memory.getObject("goal l");
            opposingGoal = (GoalInfo) m_memory.getObject("goal r");
            ownPenalty_c = (FlagInfo) m_memory.getObject("flag p l c");
            ownSideLine = (LineInfo) m_memory.getObject("line l");
        }

        // store the object to act on when performing an action
        this.environmentObjects[0] = ball;
        this.environmentObjects[1] = ownGoal;
        this.environmentObjects[2] = opposingGoal;
        this.environmentObjects[5] = centre_c;
        this.environmentObjects[6] = ownPenalty_c;



		List<ObjectInfo> players = m_memory.getObjects("player");
        // Reset the current perceptions seen in the enviroment
        List<Belief> currentPerceptions = new LinkedList<Belief>();

        // Descritizing code goes here to translate the current Environment state
        // into a list of Perceptions for the agent to add to it's belief base.

        //Add beliefs about the ball.
        if( ball != null ) {
            currentPerceptions.add(Belief.BALL_SEEN);
            if( ball.m_distance < 0.75) {
                currentPerceptions.add(Belief.AT_BALL);
            }

            if(Math.abs(ball.m_direction) < 10) {
                currentPerceptions.add(Belief.FACING_BALL);
            }

            if(ball.m_direction < 0) {
                currentPerceptions.add(Belief.BALL_TO_LEFT);
            } else {
                currentPerceptions.add(Belief.BALL_TO_RIGHT);
            }

            // TODO: Check to see if you're a goalie
            if(ball.m_distance < 15){
                currentPerceptions.add(Belief.BALL_MED_DIST_FROM_GOALIE);
            }
        }

        //Add beliefs about the players own goal line.
        if(ownSideLine != null){
            currentPerceptions.add(Belief.OWN_GOAL_LINE_SEEN);
            if(ownSideLine.m_distance < 10){
                currentPerceptions.add(Belief.CLOSE_TO_OWN_GOAL_LINE);
            }

        }

        //Add beliefs about the center of the pitch.
        if(centre_c != null){
            currentPerceptions.add(Belief.CENTRE_SEEN);
            if(Math.abs(centre_c.m_direction) < 10) {
                currentPerceptions.add(Belief.FACING_CENTRE);
            }

            if(centre_c.m_direction < 0) {
                currentPerceptions.add(Belief.CENTRE_TO_LEFT);
            }else{
                currentPerceptions.add(Belief.CENTRE_TO_RIGHT);
            }

            if(centre_c.m_distance < 3){
                currentPerceptions.add(Belief.CLOSE_TO_CENTRE);
            }
        }

        //Add beliefs about the two goals and the players position in relation to them.
        if(ownGoal == null && opposingGoal == null){ }
        else{
            if(ownGoal != null){
                currentPerceptions.add(Belief.OWN_GOAL_SEEN);
                if(ownGoal.m_distance < 50.0) {
                    currentPerceptions.add(Belief.ON_OWN_SIDE);
                }
                if( ownGoal.m_distance < 2) {
                    currentPerceptions.add(Belief.AT_OWN_NET);
                }
                if(Math.abs(ownGoal.m_direction) < 10) {
                    currentPerceptions.add(Belief.FACING_OWN_GOAL);
                }
            }

            if(opposingGoal != null){
                currentPerceptions.add(Belief.ENEMY_GOAL_SEEN);
                if(opposingGoal.m_distance > 75.0) {
                    currentPerceptions.add(Belief.ON_OWN_SIDE);
                }
                if( opposingGoal.m_distance < 0.75) {
                    currentPerceptions.add(Belief.AT_OPPOSING_NET);
                }
                if(Math.abs(opposingGoal.m_direction) < 10) {
                    currentPerceptions.add(Belief.FACING_OPPOSING_GOAL);
                }

                if(opposingGoal.m_direction < 0) {
                    currentPerceptions.add(Belief.ENEMY_GOAL_TO_LEFT);
                }else{
                    currentPerceptions.add(Belief.ENEMY_GOAL_TO_RIGHT);
                }
            }
        }

        //Add beliefs about the players own penalty box and the players position relative to it.
        if(ownPenalty_c != null){
            currentPerceptions.add(Belief.OWN_PENALTY_SEEN);
            if(Math.abs(ownPenalty_c.getDirection()) < 10) {
                currentPerceptions.add(Belief.FACING_OWN_PENALTY);
            }

            if(ownPenalty_c.getDirection() < 0) {
                currentPerceptions.add(Belief.OWN_PENALTY_TO_LEFT);
            }else{
                currentPerceptions.add(Belief.OWN_PENALTY_TO_RIGHT);
            }

            if(ownPenalty_c.getDistance() < 3){
                currentPerceptions.add(Belief.CLOSE_TO_OWN_PENALTY);
            }
        }

        //Add beliefs about the balls position (which side of the pitch it is on).
        if (ball != null && ownGoal != null) {
            // using Law of Cosines to find distance c^2 = a^2 + b^2 - 2abcos(theta)
            double angle_rads = (Math.abs(ball.m_direction - ownGoal.m_direction) * Math.PI) / 180.0;
            double distance = getDistance(ball.m_distance,ownGoal.m_distance,angle_rads);
            if (distance < 50.0) {
                currentPerceptions.add(Belief.BALL_ON_OWN_SIDE);
            } else {
                currentPerceptions.add(Belief.BALL_ON_ENEMY_SIDE);
            }
        }else if (ball != null && opposingGoal != null) {
            // using Law of Cosines to find distance c^2 = a^2 + b^2 - 2abcos(theta)
            double angle_rads = (Math.abs(ball.m_direction - opposingGoal.m_direction) * Math.PI) / 180.0;
            double distance = getDistance(ball.m_distance,opposingGoal.m_distance,angle_rads);
            if (distance > 60.0) {
                currentPerceptions.add(Belief.BALL_ON_OWN_SIDE);
            } else {
                currentPerceptions.add(Belief.BALL_ON_ENEMY_SIDE);
            }
        }

        //Add beliefs about the other players on the pitch and their positions in relation to the ball.
        if(players.size() > 0){
            if(ball != null){
                double ballDistance = ball.getDistance();
                double ballDirection = ball.getDirection();
                double shorterBallDistance = ballDistance + 1;
                for (ObjectInfo currentPlayer : players) {
                    PlayerInfo player = (PlayerInfo) currentPlayer;

                    if(player.m_teamName.equals(m_team)){ // if player is a teammate
                        if(!currentPerceptions.contains(Belief.TEAMMATE_AVAILABLE)){
                            currentPerceptions.add(Belief.TEAMMATE_AVAILABLE);
                            this.environmentObjects[3] = player;
                        }
                        // using Law of Cosines to find distance c^2 = a^2 + b^2 - 2abcos(theta)
                        double angle_rads = (Math.abs(ballDirection - player.m_direction) * Math.PI) / 180.0;
                        double teammateDistance = getDistance(ballDistance,player.m_distance,angle_rads);
                        if(teammateDistance < ballDistance){ // this player is the closest to the ball
                            shorterBallDistance = teammateDistance;
                            currentPerceptions.add(Belief.TEAMMATE_CLOSER_TO_BALL);
                            if(shorterBallDistance < 0.75){
                                currentPerceptions.add(Belief.TEAMMATE_AT_BALL);
                            }
                        }
                    }else{ // if player is on opposing team
                        if(player.m_distance < 1 && ball.m_distance < 0.5){
                            this.environmentObjects[4] = player;
                            currentPerceptions.add(Belief.ENEMY_AT_BALL);
                        }
                        if(player.m_distance < 10 && opposingGoal != null ){
                            if(Math.abs(opposingGoal.m_direction - player.m_direction) < 3){
                                this.environmentObjects[4] = player;
                                currentPerceptions.add(Belief.ENEMY_BLOCKING_SHOT);
                            }
                        }
                    }
                }
            }
        }
        return currentPerceptions;
    }


    /**
     * @brief This function takes in the BDI Agents current intent and sends an
     * action to Krislet for the player to perform on the server.
     * @param intent Intent (action) that the BDI agent has identified through
     * its reasoning cycle.
     */
    public void performIntent(Intent intent) {

        //Memory objects that the player will need to perform certain actions.
		BallInfo ball = (BallInfo) environmentObjects[0];
        GoalInfo ownGoal = (GoalInfo) environmentObjects[1];;
        GoalInfo opposingGoal = (GoalInfo) environmentObjects[2];;
        PlayerInfo player = (PlayerInfo) environmentObjects[3];
        PlayerInfo enemy =  (PlayerInfo) environmentObjects[4];
        FlagInfo centre = (FlagInfo) environmentObjects[5];
        FlagInfo ownPenalty = (FlagInfo) environmentObjects[6];

        try {
            switch(intent){
                case KICK_AT_NET:
                    m_krislet.kick(75, opposingGoal.m_direction);
                    break;
                case KICK_TO_PLAYER:
                    m_krislet.kick(75, player.m_direction);
                    break;
                case KICK_TO_DEFEND:
                    m_krislet.kick(75, 180);
                    break;
                case KICK_TO_SIDE:
                    m_krislet.kick(25, 45);
                    m_krislet.turn(45);
                    break;
                case KICK_STRAIGHT:
                    m_krislet.kick(50,0);
                    break;
                case LOOK_LEFT:
                    m_krislet.turn(-80);
                    m_memory.waitForNewInfo();
                    break;
                case LOOK_RIGHT:
                    m_krislet.turn(80);
                    m_memory.waitForNewInfo();
                    break;
                case TURN_TO_BALL:
                    m_krislet.turn(ball.getDirection());
                    m_memory.waitForNewInfo();
                    break;
                case TURN_TO_OWN_GOAL:
                    m_krislet.turn(ownGoal.getDirection());
                    break;
                case TURN_TO_OPPOSING_GOAL:
                    m_krislet.turn(opposingGoal.getDirection());
                    break;
                case TURN_UP_FIELD:
                    m_krislet.turn(opposingGoal.getDirection() + 9);
                    break;
                case TURN_TO_PLAYER:
                    m_krislet.turn(player.getDirection());
                    break;
                case TURN_TO_CENTRE:
                    m_krislet.turn(centre.getDirection());
                    break;
                case TURN_TO_OWN_PENALTY:
                    m_krislet.turn(ownPenalty.getDirection());
                    break;
                case RUN_TO_PLAYER:
                    m_krislet.dash(100*player.getDistance());
                    break;
                case RUN_TO_BALL:
                    if(ball.getDistance() > 5){
                        m_krislet.dash(50);
                    }else{
                        m_krislet.dash(50*ball.getDistance());
                    }
                    break;
                case RUN_TO_OWN_GOAL:
                    m_krislet.dash(100*ownGoal.getDistance());
                    break;
                case RUN_TO_OPPOSING_GOAL:
                    m_krislet.dash(100*opposingGoal.getDistance());
                    break;
                case RUN_TO_CENTRE:
                    m_krislet.dash(100*centre.getDistance());
                    break;
                case RUN_TO_OWN_PENALTY:
                    m_krislet.dash(100*ownPenalty.getDistance());
                    break;
                case RUN_UP_FIELD:
                    m_krislet.dash(100*opposingGoal.getDistance());
                    break;
                case WAIT:
                    m_memory.waitForNewInfo();
                    break;
                default:
                System.out.printf("UNKNOWN INTENT (%s) - WAITING", intent);
                    m_memory.waitForNewInfo();
                    break;
            }
        } catch (Exception e) {
            System.out.printf("INTENT FAILED (%s)", intent);
            m_memory.waitForNewInfo();
        }
    }

    /**
     * @brief The main loop for the agent.
     * @details This function runs internally while the game is running, controlling
     * the player based on the current environement state, JasonAgent and outputs.
     * The while loop constantly descritizes envrionment into "Beliefs", sends those
     * beliefs to the JasonAgent to get an intent, and then undescritizes that
     * intent into an action to send to Krislet.
    */
    public void run()
    {
        // Establish Agent
        JasonAgent agent = new JasonAgent(this.m_agent_asl);
        System.out.println("BDI Agent Loaded: Begining new game of RoboCup");


    	// first put it somewhere on my side
    	if(Pattern.matches("^before_kick_off.*",m_playMode))
    	   move();

    	while( !m_timeOver ){
    		// sleep one step to ensure that we will not send
    		// two commands in one cycle.
    		try{
    		    Thread.sleep(1*SoccerParams.simulator_step);
    		}catch(Exception e){
    	    }

            // Get current perceptions
            perceptions = this.getPerceptions();

            //for (ObjectInfo currentPlayer : players) {
            // Get an intent from the Jason Agent based on this cycles new
            // current perceptions so we can perform an action
            //System.out.println("Starting Reasoning:");
            //System.out.println(perceptions.toString());
            Intent intent = agent.getIntent(perceptions);

            //System.out.println("Got Intent:");
            //System.out.println(intent.toString());
            // Perform the action
            this.performIntent(intent);
        }

    	m_krislet.bye();
    }

    /**
     * @brief Operation move determines the movement speed for each type of
     * agent then performs the move action.
     */
    public void move(){
        if(m_agent_asl.equals("AgentSpecifications/defender.asl")){
            m_krislet.move( -34 , 0 );
        }else if(m_agent_asl.equals("AgentSpecifications/goalie.asl")){
            m_krislet.move( -51 , 0 );
        }else if(m_agent_asl.equals("AgentSpecifications/midfielder.asl")){
            m_krislet.move( -15 , 0);
        }else if(m_agent_asl.equals("AgentSpecifications/attacker.asl")){
            m_krislet.move( -5 , Math.random() * 10 - 5);
        }else{
            m_krislet.move( -Math.random()*20.5 , Math.random()*30.0 );
        }

    }


    // using Law of Cosines to find distance c^2 = a^2 + b^2 - 2abcos(theta)
    public double getDistance(double a, double b, double theta){
        return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2) - (2.0 * a * b * Math.cos(theta)));
    }


    //===========================================================================
    // Here are suporting functions for implement logic


    //===========================================================================
    // Implementation of SensorInput Interface

    //---------------------------------------------------------------------------
    // This function sends see information
    public void see(VisualInfo info)
    {
	m_memory.store(info);
    }


    //---------------------------------------------------------------------------
    // This function receives hear information from player
    public void hear(int time, int direction, String message)
    {

    }

    //---------------------------------------------------------------------------
    // This function receives hear information from referee
    public void hear(int time, String message){
        if(message.compareTo("time_over") == 0){
            m_timeOver = true;
        }
    }
}
