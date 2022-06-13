package me.ghosttypes.ghostware.utils;


import me.ghosttypes.ghostware.utils.misc.MathUtil;
import me.ghosttypes.ghostware.utils.misc.StringHelper;

import java.util.ArrayList;

public class OutgoingMessages {

    public static ArrayList<EzMessage> ezMessages;
    public static ArrayList<EzMessage> ezMessagesRemoveQueue;

    public static void init() {
        // init lists or cope
        ezMessages = new ArrayList<>();
        ezMessagesRemoveQueue = new ArrayList<>();
    }

    public static void updateQueue() {
        // check ez messages waiting in queue
        ezMessages.forEach(OutgoingMessages::checkEz);
        // remove after iteration or crash cope
        ezMessagesRemoveQueue.forEach(ezMessage -> ezMessages.remove(ezMessage));
    }

    public static void checkEz(EzMessage ezMessage) {
        ezMessage.tick(); // tick the entry
        if (ezMessage.ticksLeft <= 0) { // if it's ready, send it + queue for removal
            ezMessagesRemoveQueue.add(ezMessage);
            sendEz(ezMessage);
        }
    }


    public static void sendEz(EzMessage ezMessage) { // send an ez message
        Wrapper.sendMessage(ezMessage.message);
        if (ezMessage.sendDM) Wrapper.messagePlayer(ezMessage.playerName, StringHelper.stripName(ezMessage.playerName, ezMessage.message));
    }

    public static class EzMessage {
        public final String message;
        public final String playerName;
        public final boolean sendDM;
        public int ticksLeft;

        public EzMessage(String player, String ezMessage, int delay, boolean sendToDm) {
            playerName = player;
            message = ezMessage;
            ticksLeft = MathUtil.intToTicks(delay);
            sendDM = sendToDm;
        }

        public void tick() {
            ticksLeft--;
        }
    }
}
