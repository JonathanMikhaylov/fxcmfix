package com.fxcm;

import quickfix.LogFactory;
import quickfix.MemoryStoreFactory;
import quickfix.MessageStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.field.SubscriptionRequestType;
import quickfix.fix44.Logout;
import quickfix.fix44.MessageFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Hello world!
 *
 */
public class Main {
    public static void main(String[] args) {
        if (args.length == 1) {
            String config = args[0];
            try {
                SessionSettings settings = new SessionSettings(config);
                String username = settings.getString("username");
                String password = settings.getString("password");

                FIXApp app = new FIXApp(username, password);
                MessageStoreFactory storeFactory = new MemoryStoreFactory();
                LogFactory logFactory = new ScreenLogFactory(settings);
                MessageFactory messageFactory = new MessageFactory();
                SocketInitiator initiator = new SocketInitiator(app, storeFactory, settings, logFactory, messageFactory);
                initiator.start();
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    String str = in.readLine();
                    if (str != null) {
                        if ("o".equalsIgnoreCase(str.trim())) {
                            app.sendOrder();
                        } else if ("ol".equalsIgnoreCase(str.trim())) {
                            app.sendOrderList();
                        } else if ("eo".equalsIgnoreCase(str.trim())) {
                            app.sendEntryOrder();
                        } else if ("or".equalsIgnoreCase(str.trim())) {
                            app.setOpenRange();
                        } else if ("oto".equalsIgnoreCase(str.trim())) {
                            app.setOTO();
                        } else if ("pq".equalsIgnoreCase(str.trim())) {
                            app.setPreviouslyQuoted();
                        } else if ("m".equalsIgnoreCase(str.trim())) {
                            app.setPrintMDS(!app.isPrintMDS());
                        } else if ("rr".equalsIgnoreCase(str.trim())) {
                            app.sendResendRequest();
                        } else if ("ci".equalsIgnoreCase(str.trim())) {
                            app.getAccounts();
                        } else if ("tr".equalsIgnoreCase(str.trim())) {
                            app.sendTestRequest();
                        } else if ("ur".equalsIgnoreCase(str.trim())) {
                            app.sendUserRequest();
                        } else if ("osr".equalsIgnoreCase(str.trim())) {
                            app.sendOrderStatusRequest();
                        } else if ("l".equalsIgnoreCase(str.trim())) {
                            app.send(new Logout());
                        } else if ("sub".equalsIgnoreCase(str.trim())) {
                            app.sendMarketDataRequest(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES);
                        } else if ("unsub".equalsIgnoreCase(str.trim())) {
                            app.sendMarketDataRequest(SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE_REQUEST);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Error: Supply configuration file");
        }
    }
}
