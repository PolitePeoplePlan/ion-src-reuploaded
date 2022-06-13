package me.ghosttypes.ghostware.utils.misc;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ThreadUtil {

    public static Executor fixedExecutor = Executors.newFixedThreadPool(20);
    public static Executor cachedExecutor = Executors.newCachedThreadPool();

}
