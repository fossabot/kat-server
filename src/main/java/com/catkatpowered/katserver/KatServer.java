package com.catkatpowered.katserver;

import com.catkatpowered.katserver.event.Event;
import com.catkatpowered.katserver.event.KatEventManager;
import com.catkatpowered.katserver.event.RegisteredListener;
import com.catkatpowered.katserver.event.interfaces.Listener;
import com.catkatpowered.katserver.extension.KatExtension;
import com.catkatpowered.katserver.extension.KatExtensionManager;
import com.catkatpowered.katserver.log.KatLoggerManager;
import com.catkatpowered.katserver.message.KatUniMessageTypeManager;
import java.io.File;
import org.apache.logging.log4j.Logger;

/**
 * Kat API 入口
 *
 * @author hanbings
 * @author suibing112233
 */
@SuppressWarnings("unused")
public class KatServer {

    // EventBus API
    public static final class KatEventBusAPI {

        public void registerEvent(Event event) {
            KatEventManager.registerEvent(event);
        }

        public static void unregisterEvent(Event event) {
            KatEventManager.unregisterEvent(event);
        }

        public static void registerListener(Listener listener) {
            KatEventManager.registerListener(listener);
        }

        public static void unregisterListener(Listener listener) {
            KatEventManager.unregisterListener(listener);
        }

        public static void callEvent(Event event) {
            KatEventManager.callEvent(event);
        }

        public static RegisteredListener getEventHandler(Event event) {
            return KatEventManager.getEventHandler(event);
        }
    }

    // 扩展 API
    public static final class KatExtensionAPI {

        public static void loadExtensions() {
            KatExtensionManager.loadExtensions();
        }

        public static void loadExtension(File jar) {
            KatExtensionManager.loadExtension(jar);
        }

        public static void unloadExtensions() {
            KatExtensionManager.unloadExtensions();
        }

        public static void unloadExtension(String extension) {
            KatExtensionManager.unloadExtension(extension);
        }

        public static void unloadExtension(KatExtension extension) {
            KatExtensionManager.unloadExtension(extension);
        }
    }

    // 日志 API
    public static final class KatLoggerAPI {

        public static Logger getLogger() {
            return KatLoggerManager.getLogger();
        }

        public static Logger getLogger(String loggerName) {
            return KatLoggerManager.getLogger(loggerName);
        }
    }

    // KatUniMessage 类型API
    public static final class KatUniMessageTypeAPI {

        public static boolean addMessageType(String msgType) {
            return KatUniMessageTypeManager.addMessageType(msgType);
        }
    }
}
