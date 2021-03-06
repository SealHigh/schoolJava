package game;
import exceptions.NoSuchCardException;
import player.Player;
import player.Table;

import java.util.Iterator;

/**
 * Created by Martin on 2016-09-10.
 */
public class Game {

    private IOHandler IOHandler;
    private Table table;

    public Game(){
        table = new Table();
        IOHandler = new IOHandler();
    }



    private void dealCard(Player player){
        try {
            player.getHand().addCard(table.getDeck().dealCard());
        }
        catch (NoSuchCardException NS){
            System.out.println(NS.getMessage());
            table.getDeck().fillDeck();
            table.getDeck().shuffleCards();
            player.getHand().addCard(table.getDeck().dealCard());
            System.out.println("Filled deck with new cards");
        }
    }
    public void checkCredit(){
        for (Iterator<Player> iterator = table.getTable().iterator(); iterator.hasNext();) {
            Player player = iterator.next();
            if (player.getCredit() < 100) {
                iterator.remove();
                IOHandler.displayPlayerKicked(player);
            }
        }
    }
    private void checkAction(){
        /**
         * Keeps asking player for next action until they either bust,
         * wants to stay or leaves the table.
         */
        for (Iterator<Player> iterator = table.getTable().iterator(); iterator.hasNext();)  //Allows removal in list while iterating
        {
            Player player = iterator.next();
            boolean n = true;
            if (player.getID() == 0) //Dealer doesn't play
                n = false;
            else if (!getBetAction(player)) { //If player wants to leave
                n = false;
                iterator.remove();
            }
            if(n)
                IOHandler.displayPlayer(player); //Player currently plaing
            while (n) {
                IOHandler.displayHand(player);
                n = getAction(player);

                if (player.checkLoseCondition()) { //If player busts display it and end players turn
                    IOHandler.displayHand(player);
                    IOHandler.displayPlayerBust(player);
                    break;
                }
            }
        }
    }

    private boolean continueGame(){
        while(true){
            IOHandler.displayContinueQ();
            int n = IOHandler.getInt();
            if (n == 0)
                return false;
            if (n == 1 && table.getSize() > 1) {
                for(int i = 0; i < 20; i++)
                    System.out.println("");
                return true;
            }
            else if(n==1)
                IOHandler.displayContinueError();
            if(n == 2)
                if(table.addPlayer())
                    IOHandler.displayPlayerAdded();
                else
                    IOHandler.displayPlayerAddError();
        }
    }

    private boolean getBetAction(Player player){
        IOHandler.displayCredit(player);
        int n  = 0;
        while(n<100 || n>2000 || n > player.getCredit()) {
            IOHandler.displayBetQ();
            n = IOHandler.getInt();
            if (n == 0)
            {
                IOHandler.displayPlayerLeft(player);
                return false;
            }
            if(n>player.getCredit())
                IOHandler.displayOutOfCredit();
        }
        player.setCurrentBet(n);
        player.subtractCredit(n);
        IOHandler.displayCredit(player);
        return true;
    }

    private boolean getAction(Player player) {
        while(true){
            if(player.getHand().getCardValues() <12 && player.getHand().getCardValues() >7)
                IOHandler.displayActionDDown();
            else
                IOHandler.displayAction();
            int n = IOHandler.getInt();
            if (n == 0) //If stay, stop asking for cards and move to next player
                return false;
            else if (n == 1) { //If hit add card and keep on asking for action
                    dealCard(player);
                return true;
            }
            else if(n == 2 && player.getHand().getCardValues() <12 && player.getHand().getCardValues() >7){
                //Double down, player doubles his bets and can only draw one more card, can only be done on 7,8,9,10,11 (standard blackjack rules)
                if(player.getCurrentBet() < player.getCredit())
                {
                    IOHandler.displayDoublingDown();
                    player.subtractCredit(player.getCurrentBet());
                    player.setCurrentBet(player.getCurrentBet()*2);
                    dealCard(player);
                    IOHandler.displayHand(player);
                    return false;
                }
                else //If player don't have enough credit to DD
                    IOHandler.displayDDError(player);
            }
            else if(n == 3){
                IOHandler.displayCredit(player);
            }
        }
    }

    private void dealerPlay(){
        /**
         * Dealer draws card until he has 17 or more
         * this is standard blackjack rules.
         */
        while(table.getDealer().getHand().getCardValues() < 17 && table.getTable().size()>1 ){
            dealCard(table.getDealer());
            IOHandler.displayDealer(table.getDealer());
            if(table.getDealer().checkLoseCondition()){
                IOHandler.displayDealerBust();
            }
        }
    }

    private void handleWinner(){
        /**
         * getWinners() sets a bool in each player to true if they won
         * then loop through each player awarding those who has won.
         * If no one won dealer won.
         */
        boolean dealerWon = true;
        table.getWinners();
        for (Player player: table.getTable()
             ) {
            if(player.isRoundWinner() && player.getID() != 0) {
                IOHandler.displayWinner(player, table.getDealer());
                player.addCredit(player.getCurrentBet()*2);
                dealerWon = false;
            }
            if (player.isRoundDraw() && player.getID() != 0){
                IOHandler.displayDraw(player, table.getDealer());
                player.addCredit(player.getCurrentBet());
                dealerWon = false;
            }
        }
        if(dealerWon && table.getTable().size() > 1) //If no one beat the dealer
            IOHandler.displayDealerWon(table.getDealer());
    }

    private boolean handleEndOfGame(){
        if(continueGame()) //Check if game should continue
            table.resetTable();
        else {
            IOHandler.displayFinalScores(table.getTable()); //Print out final credits
            return true;
        }
        return false;
    }

    public void startGame() {
        /**
         * Initializes table with given amount of players.
         * Then loops through checking players actions,
         * removing and adding new players, and displaying
         * all essential data.
         */
        int j = IOHandler.getNoPlayers();
        table.initTable(j);
        while (true) {
            IOHandler.displayDealer(table.getDealer()); //Show dealers initial hand
            checkAction();  //Check what each player wants to do
            dealerPlay();
            handleWinner();
            checkCredit(); // Make sure all players have credit left, if not remove them

            if(handleEndOfGame()) //If we want to quit break
                break;
        }
    }
}
