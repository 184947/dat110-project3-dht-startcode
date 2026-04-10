/**
 *
 */
package no.hvl.dat110.middleware;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.LamportClock;
import no.hvl.dat110.util.Util;

/**
 * @author tdoy
 *
 */
public class MutualExclusion {

    private static final Logger logger = LogManager.getLogger(MutualExclusion.class);

    /** lock variables */
    private boolean CS_BUSY = false;
    private boolean WANTS_TO_ENTER_CS = false;
    private List<Message> queueack;
    private List<Message> mutexqueue;

    private LamportClock clock;
    private Message ownRequest;
    private Node node;

    public MutualExclusion(Node node) throws RemoteException {
        this.node = node;

        clock = new LamportClock();
        queueack = new ArrayList<Message>();
        mutexqueue = new ArrayList<Message>();
    }

    public synchronized void acquireLock() {
        CS_BUSY = true;
    }

    public void releaseLocks() {
        WANTS_TO_ENTER_CS = false;
        CS_BUSY = false;

        for (Message msg : mutexqueue) {
            try {
                NodeInterface stub = Util.getProcessStub(msg.getNodeName(), msg.getPort());
                if (stub != null) {
                    msg.setAcknowledged(true);
                    stub.onMutexAcknowledgementReceived(msg);
                }
            } catch (RemoteException e) {
                // ignore
            }
        }

        mutexqueue.clear();
        ownRequest = null;
    }

    public boolean doMutexRequest(Message message, byte[] updates) throws RemoteException {

        logger.info(node.nodename + " wants to access CS");

        queueack.clear();
        mutexqueue.clear();

        clock.increment();
        message.setClock(clock.getClock());
        ownRequest = message;

        WANTS_TO_ENTER_CS = true;

        List<Message> voters = removeDuplicatePeersBeforeVoting();

        multicastMessage(message, voters);

        boolean permission = false;
        int attempts = 0;

        while (!permission && attempts < 500) {
            permission = areAllMessagesReturned(voters.size());
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            attempts++;
        }

        if (permission) {
            acquireLock();
            node.broadcastUpdatetoPeers(updates);
            multicastReleaseLocks(new java.util.HashSet<>(voters));
        }

        return permission;
    }

    // multicast message to other processes including self
    private void multicastMessage(Message message, List<Message> activenodes) throws RemoteException {

        logger.info("Number of peers to vote = " + activenodes.size());

        for (Message peer : activenodes) {
            NodeInterface stub = Util.getProcessStub(peer.getNodeName(), peer.getPort());

            if (stub != null) {
                stub.onMutexRequestReceived(message);
            }
        }
    }

    public void onMutexRequestReceived(Message message) throws RemoteException {

        clock.adjust(message.getClock());

        if (message.getNodeName().equals(node.getNodeName())) {
            message.setAcknowledged(true);
            onMutexAcknowledgementReceived(message);
            return;
        }

        int caseid = -1;

        if (!CS_BUSY && !WANTS_TO_ENTER_CS) {
            caseid = 0;
        } else if (CS_BUSY) {
            caseid = 1;
        } else if (!CS_BUSY && WANTS_TO_ENTER_CS) {
            caseid = 2;
        }

        doDecisionAlgorithm(message, mutexqueue, caseid);
    }

    public void doDecisionAlgorithm(Message message, List<Message> queue, int condition) throws RemoteException {

        String procName = message.getNodeName();
        int port = message.getPort();

        switch (condition) {

            case 0: {
                NodeInterface stub = Util.getProcessStub(procName, port);

                if (stub != null) {
                    message.setAcknowledged(true);
                    stub.onMutexAcknowledgementReceived(message);
                }

                break;
            }

            case 1: {
                queue.add(message);
                break;
            }

            case 2: {

                int senderClock = message.getClock();
                int ownClock = ownRequest.getClock();

                BigInteger senderID = message.getNodeID();
                BigInteger ownID = node.getNodeID();

                boolean senderWins = false;

                if (senderClock < ownClock) {
                    senderWins = true;
                } else if (senderClock == ownClock) {
                    if (senderID.compareTo(ownID) < 0) {
                        senderWins = true;
                    }
                }

                if (senderWins) {
                    NodeInterface stub = Util.getProcessStub(procName, port);

                    if (stub != null) {
                        message.setAcknowledged(true);
                        stub.onMutexAcknowledgementReceived(message);
                    }
                } else {
                    queue.add(message);
                }

                break;
            }

            default:
                break;
        }
    }

    public void onMutexAcknowledgementReceived(Message message) throws RemoteException {
        queueack.add(message);
    }

    // multicast release locks message to other processes including self
    public void multicastReleaseLocks(Set<Message> activenodes) {
        logger.info("Releasing locks from = " + activenodes.size());

        for (Message peer : activenodes) {
            NodeInterface stub = Util.getProcessStub(peer.getNodeName(), peer.getPort());

            if (stub != null) {
                try {
                    stub.releaseLocks();
                } catch (RemoteException e) {
                    // ignore unavailable peer
                }
            }
        }
    }

    private boolean areAllMessagesReturned(int numvoters) throws RemoteException {
        logger.info(node.getNodeName() + ": size of queueack = " + queueack.size());

        boolean all = (queueack.size() >= numvoters);

        if (all) {
            queueack.clear();
            return true;
        }

        return false;
    }

    private List<Message> removeDuplicatePeersBeforeVoting() {

        List<Message> uniquepeer = new ArrayList<Message>();
        for (Message p : node.activenodesforfile) {
            boolean found = false;
            for (Message p1 : uniquepeer) {
                if (p.getNodeName().equals(p1.getNodeName())) {
                    found = true;
                    break;
                }
            }
            if (!found)
                uniquepeer.add(p);
        }
        return uniquepeer;
    }
}
