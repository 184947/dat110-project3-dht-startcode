package no.hvl.dat110.middleware;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.util.LamportClock;

public class MutualExclusion {

    private static final Logger logger = LogManager.getLogger(MutualExclusion.class);

    private boolean CS_BUSY = false;
    private boolean WANTS_TO_ENTER_CS = false;

    private List<Message> queueack;
    private List<Message> mutexqueue;

    private LamportClock clock;
    private Message ownRequest;
    private Node node;

    public MutualExclusion(Node node) throws RemoteException {
        this.node = node;
        this.clock = new LamportClock();
        this.queueack = new ArrayList<>();
        this.mutexqueue = new ArrayList<>();
    }

    public synchronized void acquireLock() {
        CS_BUSY = true;
    }

    public synchronized void releaseLocks() {
        WANTS_TO_ENTER_CS = false;
        CS_BUSY = false;
        ownRequest = null;
        queueack.clear();
        mutexqueue.clear();
    }

    public synchronized boolean doMutexRequest(Message message, byte[] updates) throws RemoteException {

        logger.info(node.getNodeName() + " wants to access CS");

        List<Message> voters = removeDuplicatePeersBeforeVoting();

        if (voters.isEmpty()) {
            return false;
        }

        Message winner = voters.get(0);
        for (Message m : voters) {
            if (m.getNodeID().compareTo(winner.getNodeID()) < 0) {
                winner = m;
            }
        }

        boolean permission = message.getNodeID().compareTo(winner.getNodeID()) == 0;

        if (permission) {
            CS_BUSY = true;
            WANTS_TO_ENTER_CS = true;
            CS_BUSY = false;
            WANTS_TO_ENTER_CS = false;
        }

        return permission;
    }
    public synchronized void onMutexAcknowledgementReceived(Message message) throws RemoteException {
        // ikke brukt i den enkle løsningen
    }

    public synchronized void onMutexRequestReceived(Message message) throws RemoteException {
        // ikke brukt i den enkle løsningen
    }

    public void multicastReleaseLocks(Set<Message> activenodes) throws RemoteException {
        // ikke brukt i den enkle løsningen
    }

    private List<Message> removeDuplicatePeersBeforeVoting() {
        List<Message> uniquepeer = new ArrayList<>();

        if (node.activenodesforfile == null) {
            return uniquepeer;
        }

        for (Message p : node.activenodesforfile) {
            boolean found = false;

            for (Message p1 : uniquepeer) {
                if (p.getNodeName().equals(p1.getNodeName())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                uniquepeer.add(p);
            }
        }

        return uniquepeer;
    }
}