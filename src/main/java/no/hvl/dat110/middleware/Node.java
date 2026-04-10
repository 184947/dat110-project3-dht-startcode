package no.hvl.dat110.middleware;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.hvl.dat110.chordoperations.ChordLookup;
import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.Hash;

public class Node extends UnicastRemoteObject implements NodeInterface {

    private BigInteger nodeID;
    protected String nodename;
    private int port;
    private NodeInterface successor;
    private NodeInterface predecessor;
    private Set<BigInteger> keys;

    private List<NodeInterface> fingerTable;
    private Map<BigInteger, Message> filesMetadata;

    protected Set<Message> activenodesforfile;

    private UpdateOperations updater;
    private ChordLookup lookup;
    private Message message;
    private MutualExclusion mutex;

    private static final long serialVersionUID = 1L;

    public Node(String nodename, int port) throws RemoteException {
        super();
        this.port = port;
        this.nodename = nodename;
        this.nodeID = Hash.hashOf(nodename);

        keys = new HashSet<>();
        fingerTable = new ArrayList<>();
        filesMetadata = new HashMap<>();
        updater = new UpdateOperations(this, filesMetadata);
        lookup = new ChordLookup(this);

        message = new Message(nodeID, nodename, port);
        mutex = new MutualExclusion(this);
    }

    @Override
    public BigInteger getNodeID() throws RemoteException {
        return nodeID;
    }

    @Override
    public String getNodeName() {
        return nodename;
    }

    @Override
    public int getPort() throws RemoteException {
        return port;
    }

    @Override
    public void setSuccessor(NodeInterface succ) throws RemoteException {
        successor = succ;
    }

    @Override
    public void setPredecessor(NodeInterface pred) {
        predecessor = pred;
    }

    @Override
    public NodeInterface getPredecessor() throws RemoteException {
        return predecessor;
    }

    @Override
    public NodeInterface getSuccessor() throws RemoteException {
        return successor;
    }

    @Override
    public Set<BigInteger> getNodeKeys() throws RemoteException {
        return keys;
    }

    @Override
    public void addKey(BigInteger id) throws RemoteException {
        keys.add(id);
    }

    @Override
    public void removeKey(BigInteger id) throws RemoteException {
        keys.remove(id);
    }

    @Override
    public List<NodeInterface> getFingerTable() {
        return fingerTable;
    }

    @Override
    public NodeInterface findSuccessor(BigInteger key) throws RemoteException {
        return lookup.findSuccessor(key);
    }

    public void copyKeysFromSuccessor(NodeInterface succ) {
        lookup.copyKeysFromSuccessor(succ);
    }

    @Override
    public void notify(NodeInterface pred_new) throws RemoteException {
        lookup.notify(pred_new);
    }

    @Override
    public Message getFilesMetadata(BigInteger fileID) throws RemoteException {
        return filesMetadata.get(fileID);
    }

    @Override
    public Map<BigInteger, Message> getFilesMetadata() throws RemoteException {
        return filesMetadata;
    }

    @Override
    public void updateFileContent(List<Message> updates) throws RemoteException {
        updater.updateFileContent(updates);
    }

    @Override
    public synchronized void broadcastUpdatetoPeers(byte[] bytesOfFile) throws RemoteException {
        updater.broadcastUpdatetoPeers(activenodesforfile, bytesOfFile);
    }

    @Override
    public void saveFileContent(String filename, BigInteger fileID, byte[] bytesOfFile, boolean primary)
            throws RemoteException {
        updater.saveFileContent(filename, fileID, bytesOfFile, primary);
    }

    @Override
    public void requestRemoteWriteOperation(byte[] updates, Set<Message> activenodes) throws RemoteException {
        this.activenodesforfile = activenodes;
        broadcastUpdatetoPeers(updates);
    }

    @Override
    public boolean requestMutexWriteOperation(Message message, byte[] updates, Set<Message> activepeers)
            throws RemoteException {
        this.message = message;
        this.activenodesforfile = activepeers;
        return mutex.doMutexRequest(message, updates);
    }

    @Override
    public void acquireLock() throws RemoteException {
        mutex.acquireLock();
    }

    @Override
    public void releaseLocks() throws RemoteException {
        mutex.releaseLocks();
    }

    @Override
    public void onMutexAcknowledgementReceived(Message message) throws RemoteException {
        mutex.onMutexAcknowledgementReceived(message);
    }

    @Override
    public void onMutexRequestReceived(Message message) throws RemoteException {
        mutex.onMutexRequestReceived(message);
    }

    @Override
    public void multicastReleaseLocks(Set<Message> activenodes) throws RemoteException {
        mutex.multicastReleaseLocks(activenodes);
    }

    public Message getMessage() {
        return message;
    }
}