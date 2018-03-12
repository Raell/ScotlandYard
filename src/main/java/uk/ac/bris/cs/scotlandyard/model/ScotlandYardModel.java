package uk.ac.bris.cs.scotlandyard.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import uk.ac.bris.cs.gamekit.graph.Edge;

import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;
import uk.ac.bris.cs.gamekit.graph.Node;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {
    
    private List<Boolean> rounds;
    private Graph<Integer, Transport> graph;
    private ScotlandYardPlayer mrX;
    private Set<Colour> winningPlayer;
    private List<ScotlandYardPlayer> detectives;
    private List<Colour> players;
    private List<Spectator> spectators;
    private Colour currentPlayer;
    private int currentRound;
    private boolean gameOver;

    public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
                    PlayerConfiguration mrX, PlayerConfiguration firstDetective,
                    PlayerConfiguration... restOfTheDetectives) {
            // TODO
            if(rounds.isEmpty()) {
                throw new IllegalArgumentException("Empty rounds");
            }
            this.rounds = requireNonNull(rounds);
            this.currentRound = NOT_STARTED;
            
            if(graph.isEmpty()) {
                throw new IllegalArgumentException("Empty map");
            }
            this.graph = requireNonNull(graph);
            
            this.winningPlayer = new HashSet<>();
            this.currentPlayer = Colour.BLACK;
            players = new ArrayList<>();
            spectators = new ArrayList<>();
            gameOver = false;
            
            this.detectives = new ArrayList<>();
            requireNonNull(firstDetective);
            detectives.add(firstDetective.toScotlandYardPlayer());
            for(PlayerConfiguration detective : restOfTheDetectives) {
                requireNonNull(detective);
                detectives.add(detective.toScotlandYardPlayer());
            }
            
            requireNonNull(mrX);
            if(mrX != null)
                playersValid(mrX.toScotlandYardPlayer(), detectives);
            
            
    }
         
    private Set<Integer> detectiveLocations(){
        Set<Integer> locs = new HashSet<>();
        for(ScotlandYardPlayer detective : detectives) {
            locs.add(detective.location());
        }
        return locs;
    }
    
    private void playersValid(ScotlandYardPlayer mrX, List<ScotlandYardPlayer> detectives) {
        
        
        if(mrX.isDetective()) { // or mr.colour.isDetective()
            throw new IllegalArgumentException("MrX should be Black");
        }
        
        this.mrX = mrX;
        mrXTicketValid(mrX.tickets());
        players.add(Colour.BLACK);
        
        for(ScotlandYardPlayer detective : detectives) {
            if(detective.isMissingTickets() || detective.hasTickets(Ticket.DOUBLE) || detective.hasTickets(Ticket.SECRET))
                throw new IllegalArgumentException("Detectives cannot have double or secret tickets");
            players.add(detective.colour());
        }
        
        if(detectiveLocations().size() != detectives.size())
            throw new IllegalArgumentException("Player locations overlap.");
        
        
        
    } 
  
    private void mrXTicketValid(Map<Ticket, Integer> tickets) {
        
        if(mrX.isMissingTickets())
            throw new IllegalArgumentException("MrX is missing tickets");
        
        for(ScotlandYardPlayer detective : detectives) {
                if(detective.location() == mrX.location())
                    throw new IllegalArgumentException("MrX and detective(s) overlap.");
            }
    }

    @Override
    public void registerSpectator(Spectator spectator) {
            requireNonNull(spectator);
            spectators.add(spectator);
    }

    @Override
    public void unregisterSpectator(Spectator spectator) {
            requireNonNull(spectator);
            spectators.remove(spectator);
    }
    
    private boolean playerAtNode(Node<Integer> node){
        for(ScotlandYardPlayer detective : detectives) {
            if(detective.location() == node.value()) return true;
        }
        return false;
    }
    
    private Collection<Edge<Integer, Transport>> connectedEdges(Node<Integer> locNode, Graph<Integer, Transport> graph){
        //gets connected edges
        Collection<Edge<Integer, Transport>> fromEdges = graph.getEdgesFrom(locNode);
        return fromEdges;
    }
    
    private Set<Move> mrXMoves(ScotlandYardPlayer player, int location) {
          
        Set<Move> moves = new HashSet<>();
        Set<TicketMove> tMoves = new HashSet<>();
        tMoves.addAll(possibleStandardMoves(player, location, false));

        if(player.hasTickets(Ticket.SECRET)){              
            tMoves.addAll(possibleStandardMoves(player, location, true));
        } 
        
        moves.addAll(tMoves);

        if(player.hasTickets(Ticket.DOUBLE)){ 
            Set<Move> doublemoves = new HashSet<>();
            for(TicketMove firstMove : tMoves) {                                      
                
                Set<TicketMove> secondMoves = possibleStandardMoves(player, firstMove.destination(), false);
                
                if(player.hasTickets(Ticket.SECRET))
                    secondMoves.addAll(possibleStandardMoves(player, firstMove.destination(), true));
                
                for(TicketMove secondMove : secondMoves) {
                    DoubleMove doublemove = new DoubleMove(player.colour(), firstMove, secondMove);
                    if(hasValidTicket(player, doublemove))
                        doublemoves.add(doublemove);
                }
            }
            moves.addAll(doublemoves);
        } 

        return moves;
    }
    
    private Set<Move> validMoves(ScotlandYardPlayer player, int location) {
        /*Set<Move> moves = new HashSet<>();
        
        Node<Integer> locNode = graph.getNode(location);
        
        Collection<Edge<Integer, Transport>> fromEdges = connectedEdges(locNode, graph);
                 
        for(Edge<Integer, Transport> edge : fromEdges) {
            
            if(!playerAtNode(edge.destination()) && player.hasTickets(Ticket.fromTransport(edge.data())))
                moves.add(new TicketMove(player.colour(), Ticket.fromTransport(edge.data()), edge.destination().value()));
            
        }
        
        if(player.isMrX())      
            moves.addAll(mrXMoves(player, graph));           
              
        if(moves.isEmpty()) 
            if(player.isDetective()) moves.add(new PassMove(player.colour()));
            else gameOver(getDetectiveColours());
        
        return moves;*/
        Set<Move> moves = new HashSet<>();
        if(player.isMrX()) {
            moves.addAll(mrXMoves(player, location));
            if(moves.isEmpty())
                gameOver(getDetectiveColours());
        }
        else {
            moves.addAll(possibleStandardMoves(player, location, false));
            if(moves.isEmpty())
                moves.add(new PassMove(player.colour()));
        }
        
        return moves;
    }
    
    private Set<TicketMove> possibleStandardMoves(ScotlandYardPlayer player, int location, boolean secret) {
        
        Set<TicketMove> moves = new HashSet<>();
        
        Node<Integer> locNode = graph.getNode(location);
        
        Collection<Edge<Integer, Transport>> fromEdges = connectedEdges(locNode, graph);
                 
        for(Edge<Integer, Transport> edge : fromEdges) {
            
            Ticket ticket = secret ? Ticket.SECRET : Ticket.fromTransport(edge.data());
            
            TicketMove move = new TicketMove(player.colour(), ticket, edge.destination().value());
            
            if(!playerAtNode(edge.destination()) && hasValidTicket(player, move))
                moves.add(move);
            
        }      
        return moves;
        
    }
    
    private boolean hasValidTicket(ScotlandYardPlayer player, TicketMove move) {      
        return player.hasTickets(move.ticket());
    }
    
    private boolean hasValidTicket(ScotlandYardPlayer player, DoubleMove move) {      
        return (player.hasTickets(move.firstMove().ticket()) && player.hasTickets(move.secondMove().ticket()));
    }
    
    @Override
    public void startRotate() {
            //Colour player = getCurrentPlayer();
            
            players.forEach((cPlayer) -> {
                ScotlandYardPlayer sYPlayer = playerFromColour(cPlayer);
                sYPlayer.player().makeMove(this, sYPlayer.location(), validMoves(sYPlayer, sYPlayer.location()), this);             
            });
            spectators.forEach((spectator) -> {
                   spectator.onRotationComplete(this);
            });
            
    }
    
    private Set<Colour> getDetectiveColours() {
        return new HashSet<> (players.subList(1, players.size() - 1));      
    }

    @Override
    public Collection<Spectator> getSpectators() {
            return spectators;
    }

    @Override
    public List<Colour> getPlayers() {
            // TODO
            return Collections.unmodifiableList(players);
    }

    @Override
    public Set<Colour> getWinningPlayers() {
            // TODO
            return Collections.unmodifiableSet(winningPlayer);
    }

    private ScotlandYardPlayer playerFromColour(Colour colour) {
        if(colour.isMrX()) { return mrX; }
        else {
            for(ScotlandYardPlayer detective : detectives) {
                if(detective.colour() == colour) return detective;
            }
        }
        return null;
    }
    
    @Override
    public Optional<Integer> getPlayerLocation(Colour colour) {
            
        ScotlandYardPlayer player = playerFromColour(colour);
        
        if(player != null) 
            if(colour != Colour.BLACK)
                return Optional.ofNullable(player.location());
            else
                return Optional.ofNullable(0);
        else
            return Optional.empty();
            
    }

    @Override
    public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
        
        ScotlandYardPlayer player = playerFromColour(colour);
        
        if(player != null) 
            return Optional.ofNullable(player.tickets().get(ticket));
        else
            return Optional.empty();
                   
    }
    
    /*private Set<Integer> getConnectedLocs(Integer loc) {
        Node locNode = graph.getNode(loc);
        Collection<Edge> fromEdges = graph.getEdgesFrom(locNode);
        Set<Integer> setLocs = new HashSet<>();
        for(Edge edge : fromEdges) {
            setLocs.add((Integer) edge.destination().value());
        }
        return setLocs;
    }*/
    
    private void gameOver(Set<Colour> winners) {
        gameOver = true;
        winningPlayer = winners;
        spectators.forEach((spectator) -> {
           spectator.onGameOver(this, winningPlayer);
        });
    }
    
    @Override
    public boolean isGameOver() {
            //for(int i : detectiveLocations()) { System.out.println(i); }
            //if(detectiveLocations().contains(mrX.location())) return true;
            //else return false;
            return gameOver;
    }

    @Override
    public Colour getCurrentPlayer() {
            return currentPlayer;
    }

    @Override
    public int getCurrentRound() {
            return currentRound;
    }

    @Override
    public List<Boolean> getRounds() {
            return Collections.unmodifiableList(rounds);
    }

    @Override
    public Graph<Integer, Transport> getGraph() {
            return new ImmutableGraph<>(graph);
    }

    @Override
    public void accept(Move move) {
        System.out.println("Player " + move.colour() + " " + playerFromColour(move.colour()).location());
        if(!isValidMove(playerFromColour(move.colour()), move))
            throw new IllegalArgumentException("Illegal Move");
        
        nextPlayer(move.colour());             
        
        if(move.colour() == Colour.BLACK) {
            //currentRound++;
            spectators.forEach((spectator) -> {
                spectator.onRoundStarted(this, currentRound);
                spectator.onMoveMade(this, move);
             }); 
            
            //if(move.getClass() == DoubleMove.class)
                //currentRound++;      
            
            currentRound++;
        }
        else {
            spectators.forEach((spectator) -> {
                spectator.onMoveMade(this, move);
            });
            
            if(detectiveLocations().contains(mrX.location())) {
                gameOver(getDetectiveColours());
            }
        }
        ScotlandYardPlayer player = playerFromColour(move.colour());
        player.location(moveDestination(player, move));
    }
    
    private int moveDestination(ScotlandYardPlayer player, Move move) {
        if(move.getClass() == TicketMove.class)
            return moveDestination((TicketMove) move);
        else if(move.getClass() == DoubleMove.class)
            return moveDestination((DoubleMove) move);
        else
            return player.location();
    }
    
    private int moveDestination(TicketMove move) {
        return move.destination();
    }
    
    private int moveDestination(DoubleMove move) {
        return move.finalDestination();
    }
    
    
    private void nextPlayer(Colour prevPlayer) {      
        int index = players.indexOf(prevPlayer);
        if(index + 1 < players.size())
            currentPlayer =  players.get(index + 1);
        else
            currentPlayer = players.get(0);
    }
    
    private boolean isValidMove(ScotlandYardPlayer player, Move move) {
        if(move.getClass() == TicketMove.class)
            return isValidMove(player, (TicketMove) move);
        else if(move.getClass() == DoubleMove.class)
            return isValidMove(player, (DoubleMove) move);
        else
            return isValidMove(player, (PassMove) move);
    }
    
    private boolean isValidMove(ScotlandYardPlayer player, PassMove move) {
        return (validMoves(player, player.location()).contains(move));
    }
    
    private boolean isValidMove(ScotlandYardPlayer player, TicketMove move) {
        return (validMoves(player, player.location()).contains(move));
    }
    
    private boolean isValidMove(ScotlandYardPlayer player, DoubleMove move) {      
        return (validMoves(player, player.location()).contains(move));
    }

}
