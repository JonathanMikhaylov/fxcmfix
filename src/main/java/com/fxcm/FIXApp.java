/*
 * $Header:$
 *
 * Copyright (c) 2016 FXCM, LLC.
 * 55 Water Street, 50th Floor 10041
 *
 * THIS SOFTWARE IS THE CONFIDENTIAL AND PROPRIETARY INFORMATION OF
 * FXCM, LLC. ("CONFIDENTIAL INFORMATION"). YOU SHALL NOT DISCLOSE
 * SUCH CONFIDENTIAL INFORMATION AND SHALL USE IT ONLY IN ACCORDANCE
 * WITH THE TERMS OF THE LICENSE AGREEMENT YOU ENTERED INTO WITH
 * FXCM.
 *
 * Author:  Andre Mermegas
 * Created: 7/19/2016 3:08 PM
 *
 * $History: $
 */
package com.fxcm;

import quickfix.Application;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.StringField;
import quickfix.field.Account;
import quickfix.field.AccountType;
import quickfix.field.BeginSeqNo;
import quickfix.field.BidType;
import quickfix.field.ClOrdID;
import quickfix.field.ClOrdLinkID;
import quickfix.field.ClearingBusinessDate;
import quickfix.field.CollInquiryID;
import quickfix.field.EndSeqNo;
import quickfix.field.ListID;
import quickfix.field.ListSeqNo;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.MassStatusReqID;
import quickfix.field.MassStatusReqType;
import quickfix.field.NoMDEntries;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.Password;
import quickfix.field.PosReqID;
import quickfix.field.PosReqType;
import quickfix.field.Price;
import quickfix.field.Quantity;
import quickfix.field.SecondaryClOrdID;
import quickfix.field.Side;
import quickfix.field.StopPx;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TestReqID;
import quickfix.field.TimeInForce;
import quickfix.field.TotNoOrders;
import quickfix.field.TradSesReqID;
import quickfix.field.TransactTime;
import quickfix.field.UserRequestID;
import quickfix.field.UserRequestType;
import quickfix.field.UserStatus;
import quickfix.field.Username;
import quickfix.fix44.BusinessMessageReject;
import quickfix.fix44.CollateralInquiry;
import quickfix.fix44.CollateralInquiryAck;
import quickfix.fix44.CollateralReport;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.Logout;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataRequestReject;
import quickfix.fix44.MarketDataSnapshotFullRefresh;
import quickfix.fix44.MessageCracker;
import quickfix.fix44.NewOrderList;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderMassStatusRequest;
import quickfix.fix44.OrderStatusRequest;
import quickfix.fix44.PositionReport;
import quickfix.fix44.RequestForPositions;
import quickfix.fix44.RequestForPositionsAck;
import quickfix.fix44.ResendRequest;
import quickfix.fix44.SecurityStatus;
import quickfix.fix44.TestRequest;
import quickfix.fix44.TradingSessionStatus;
import quickfix.fix44.TradingSessionStatusRequest;
import quickfix.fix44.UserRequest;
import quickfix.fix44.UserResponse;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 */
public class FIXApp extends MessageCracker implements Application {
    private ClOrdID mClOrdID;
    private CollInquiryID mColInquiryID;
    private CollateralReport mCollateralReport;
    private final String mInstrument = "EUR/USD";
    private boolean mOTO;
    private boolean mOpenRange;
    private String mPassword;
    private boolean mPreviouslyQuoted;
    private boolean mPrintMDS;
    private long mRequestID;
    private SessionID mSessionID;
    private TradingSessionStatus mSessionStatus;
    private Date mStartSession;
    private Calendar mUTCCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private String mUsername;

    FIXApp(String aUsername, String aPassword) {
        mUsername = aUsername;
        mPassword = aPassword;
        mPrintMDS = false;
    }

    @Override
    public void fromAdmin(Message aMessage, SessionID aSessionID) {
        try {
            crack(aMessage, aSessionID);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    @Override
    public void fromApp(Message aMessage, SessionID aSessionID) {
        try {
            crack(aMessage, aSessionID);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public void getAccounts() {
        CollateralInquiry msg = new CollateralInquiry();
        mColInquiryID = new CollInquiryID(String.valueOf(nextID()));
        msg.set(mColInquiryID);
        msg.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
        send(msg);
    }

    private void getClosedPositions() {
        try {
            RequestForPositions rfp = new RequestForPositions();
            rfp.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
            rfp.set(new PosReqType(PosReqType.TRADES));
            rfp.set(new Account("ALL"));
            rfp.set(new TransactTime(new Date()));
            rfp.set(new AccountType(AccountType.ACCOUNT_IS_CARRIED_ON_NON_CUSTOMER_SIDE_OF_BOOKS_AND_IS_CROSS_MARGINED));
            rfp.set(new PosReqID(String.valueOf(nextID())));
            rfp.set(new ClearingBusinessDate(getDate()));
            send(rfp);
        } catch (Exception aException) {
            aException.printStackTrace();
        }
    }

    private String getDate() {
        String year = String.valueOf(mUTCCal.get(Calendar.YEAR));
        int iMonth = mUTCCal.get(Calendar.MONTH) + 1;
        String month = iMonth <= 9 ? "0" + iMonth : String.valueOf(iMonth);
        int iDay = mUTCCal.get(Calendar.DAY_OF_MONTH);
        String day = iDay <= 9 ? "0" + iDay : String.valueOf(iDay);
        return year + month + day;
    }

    private void getOpenPositions() {
        try {
            RequestForPositions rfp = new RequestForPositions();
            rfp.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
            rfp.set(new PosReqType(PosReqType.POSITIONS));
            rfp.set(new Account("ALL"));
            rfp.set(new TransactTime(new Date()));
            rfp.set(new AccountType(AccountType.ACCOUNT_IS_CARRIED_ON_NON_CUSTOMER_SIDE_OF_BOOKS_AND_IS_CROSS_MARGINED));
            rfp.set(new PosReqID(String.valueOf(nextID())));
            rfp.set(new ClearingBusinessDate(getDate()));
            send(rfp);
        } catch (Exception aException) {
            aException.printStackTrace();
        }
    }

    private void getOrders() {
        MassStatusReqID reqID = new MassStatusReqID(String.valueOf(nextID()));
        MassStatusReqType reqType = new MassStatusReqType(MassStatusReqType.STATUS_FOR_ALL_ORDERS);
        OrderMassStatusRequest oms = new OrderMassStatusRequest(reqID, reqType);
        send(oms);
    }

    public boolean isPrintMDS() {
        return mPrintMDS;
    }

    public void setPrintMDS(boolean aPrintMDS) {
        mPrintMDS = aPrintMDS;
    }

    private synchronized long nextID() {
        mRequestID++;
        if (mRequestID > 0x7FFFFFF0) {
            mRequestID = 1;
        }
        return mRequestID;
    }

    @Override
    public void onCreate(SessionID aSessionID) {
        mSessionID = aSessionID;
    }

    @Override
    public void onLogon(SessionID aSessionID) {
        mStartSession = new Date();
        System.out.println("got logon " + aSessionID);
        sendUserRequest();
    }

    @Override
    public void onLogout(SessionID aSessionID) {
        System.out.println("\n\ngot logout " + aSessionID);
        System.out.println("StartSession = " + mStartSession);
        System.out.println("StopSession = " + new Date());
        System.out.println("\n\n");
    }

    @Override
    public void onMessage(UserResponse aUserResponse, SessionID aSessionID)
            throws FieldNotFound {
        System.out.println("<< UserResponse = " + aUserResponse);
        if (aUserResponse.getInt(UserStatus.FIELD) == UserStatus.LOGGED_IN) {
            TradingSessionStatusRequest msg = new TradingSessionStatusRequest();
            msg.set(new TradSesReqID("TSSR REQUEST ID " + nextID()));
            msg.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
            send(msg);
        }
    }

    @Override
    public void onMessage(CollateralInquiryAck aCollateralInquiryAck,
                          SessionID aSessionID) {
        System.out.println("<< Collateral Inquiry Ack = " + aCollateralInquiryAck);
    }

    @Override
    public void onMessage(CollateralReport aCollateralReport,
                          SessionID aSessionID) throws FieldNotFound {
        System.out.println("<< Collateral Report = " + aCollateralReport);
        if (mColInquiryID.equals(aCollateralReport.getCollInquiryID())) {
            mCollateralReport = aCollateralReport;
        }
    }

    @Override
    public void onMessage(BusinessMessageReject aBusinessMessageReject,
                          SessionID aSessionID) {
        System.out.println("<< Business Message Reject = " + aBusinessMessageReject);
    }

    @Override
    public void onMessage(MarketDataSnapshotFullRefresh aMarketDataSnapshotFullRefresh,
                          SessionID aSessionID) {
        if (mPrintMDS || aMarketDataSnapshotFullRefresh.isSetMDReqID()) {
            System.out.println("<< MarketDataSnapshot = " + aMarketDataSnapshotFullRefresh);
        }
        try {
            Symbol symbol = aMarketDataSnapshotFullRefresh.getInstrument().getSymbol();
            if (mOTO && symbol.valueEquals(mInstrument)) {
                mOTO = false;
                NewOrderList list = new NewOrderList();
                list.set(new ListID("testlistid"));
                list.set(new BidType(BidType.NO_BIDDING_PROCESS));
                list.setField(new StringField(1385, "2"));
                NewOrderList.NoOrders order = new NewOrderList.NoOrders();
                order.set(new ClOrdID(aSessionID + "-" + System.currentTimeMillis() + "-" + Long.toString(nextID())));
                order.set(new ListSeqNo(1));
                order.set(new Side(Side.BUY));
                order.set(new TransactTime());
                order.set(new OrdType(OrdType.STOP_LIMIT));
                order.set(mCollateralReport.getAccount());
                order.set(new Symbol(mInstrument));
                order.set(new OrderQty(mCollateralReport.get(new Quantity()).getValue()));
                order.set(new SecondaryClOrdID("fix multi session test"));
                order.set(new TimeInForce(TimeInForce.IMMEDIATE_OR_CANCEL));
                NoMDEntries entries = aMarketDataSnapshotFullRefresh.getNoMDEntries();
                int value = entries.getValue();
                double price = 0;
                for (int i = 1; i <= value; i++) {
                    MarketDataSnapshotFullRefresh.NoMDEntries group = (MarketDataSnapshotFullRefresh.NoMDEntries) aMarketDataSnapshotFullRefresh.getGroup(i, new MarketDataSnapshotFullRefresh.NoMDEntries());
                    if (group.getMDEntryType().getValue() == MDEntryType.OFFER) {
                        System.out.println(group.getMDEntryType() + " = " + group.getMDEntryPx());
                        price = group.getMDEntryPx().getValue();
                    }
                }
                Price price1 = new Price(price - 0.0006);
                System.out.println("price1 = " + price1);
                order.set(price1);
                StopPx stopPx = new StopPx(price + 0.0006);
                System.out.println("stopPx = " + stopPx);
                order.set(stopPx);
                order.set(new ClOrdLinkID("1"));
                list.addGroup(order);

                NewOrderList.NoOrders limit = new NewOrderList.NoOrders();
                limit.set(new ClOrdID(aSessionID + "-" + System.currentTimeMillis() + "-" + Long.toString(nextID())));
                limit.set(new ListSeqNo(2));
                limit.set(new Side(Side.BUY));
                limit.set(new TransactTime());
                limit.set(new OrdType(OrdType.LIMIT));
                limit.set(mCollateralReport.getAccount());
                limit.set(new Symbol(mInstrument));
                limit.set(new OrderQty(mCollateralReport.get(new Quantity()).getValue()));
                limit.set(new Price(1.5));
                limit.set(new TimeInForce(TimeInForce.GOOD_TILL_CANCEL));
                limit.set(new SecondaryClOrdID("fix multi session test"));
                limit.set(new ClOrdLinkID("2"));
                list.addGroup(limit);

                NewOrderList.NoOrders stop = new NewOrderList.NoOrders();
                stop.set(new ClOrdID(aSessionID + "-" + System.currentTimeMillis() + "-" + Long.toString(nextID())));
                stop.set(new ListSeqNo(3));
                stop.set(new Side(Side.BUY));
                stop.set(new TransactTime());
                stop.set(new OrdType(OrdType.STOP));
                stop.set(mCollateralReport.getAccount());
                stop.set(new Symbol(mInstrument));
                stop.set(new OrderQty(mCollateralReport.get(new Quantity()).getValue()));
                stop.set(new StopPx(1.1));
                stop.set(new TimeInForce(TimeInForce.GOOD_TILL_CANCEL));
                stop.set(new SecondaryClOrdID("fix multi session test"));
                stop.set(new ClOrdLinkID("2"));
                list.addGroup(stop);

                list.set(new TotNoOrders(3));
                send(list);
            }

            if (mOpenRange && symbol.valueEquals(mInstrument)) {
                mOpenRange = false;
                System.out.println("<< MarketDataSnapshot = " + aMarketDataSnapshotFullRefresh);
                NewOrderSingle order = new NewOrderSingle(new ClOrdID(aSessionID + "-" + System.currentTimeMillis() + "-" + Long.toString(nextID())), new Side(Side.BUY), new TransactTime(), new OrdType(OrdType.STOP_LIMIT));
                System.out.println("-- creating order with account = " + mCollateralReport.getAccount());
                order.set(mCollateralReport.getAccount());
                order.set(new Symbol(mInstrument));
                order.set(new OrderQty(mCollateralReport.get(new Quantity()).getValue()));
                order.set(new SecondaryClOrdID("fix multi session test"));
                order.set(new TimeInForce(TimeInForce.IMMEDIATE_OR_CANCEL));
                NoMDEntries entries = aMarketDataSnapshotFullRefresh.getNoMDEntries();
                int value = entries.getValue();
                double price = 0;
                for (int i = 1; i <= value; i++) {
                    MarketDataSnapshotFullRefresh.NoMDEntries group = (MarketDataSnapshotFullRefresh.NoMDEntries) aMarketDataSnapshotFullRefresh.getGroup(i, new MarketDataSnapshotFullRefresh.NoMDEntries());
                    if (group.getMDEntryType().getValue() == MDEntryType.OFFER) {
                        System.out.println(group.getMDEntryType() + " = " + group.getMDEntryPx());
                        price = group.getMDEntryPx().getValue();
                    }
                }
                System.out.println("stream price = " + price);
                Price price1 = new Price(price - 0.0006);
                System.out.println("price = " + price1);
                order.set(price1);
                StopPx stopPx = new StopPx(price + 0.0006);
                System.out.println("stopPx = " + stopPx);
                order.set(stopPx);
                System.out.println(" >> sending order = \n" + order);
                send(order);
            } else if (mPreviouslyQuoted && symbol.valueEquals(mInstrument)) {
                mPreviouslyQuoted = false;
                System.out.println("<< MarketDataSnapshot = " + aMarketDataSnapshotFullRefresh);
                NewOrderSingle order = new NewOrderSingle(new ClOrdID(aSessionID + "-" + System.currentTimeMillis() + "-" + Long.toString(nextID())), new Side(Side.BUY), new TransactTime(), new OrdType(OrdType.MARKET));
                System.out.println("-- creating order with account = " + mCollateralReport.getAccount());
                order.set(mCollateralReport.getAccount());
                order.set(new Symbol(mInstrument));
                order.set(new OrderQty(mCollateralReport.get(new Quantity()).getValue()));
                order.set(new TimeInForce(TimeInForce.IMMEDIATE_OR_CANCEL));
                order.set(new SecondaryClOrdID("fix multi session test"));
                NoMDEntries entries = aMarketDataSnapshotFullRefresh.getNoMDEntries();
                for (int i = 1; i <= entries.getValue(); i++) {
                    MarketDataSnapshotFullRefresh.NoMDEntries group = (MarketDataSnapshotFullRefresh.NoMDEntries) aMarketDataSnapshotFullRefresh.getGroup(i, new MarketDataSnapshotFullRefresh.NoMDEntries());
                    if (group.getMDEntryType().getValue() == MDEntryType.OFFER) {
                        System.out.println(group.getMDEntryType() + " = " + group.getMDEntryPx());
                        order.set(new Price(group.getMDEntryPx().getValue()));
                        //order.set(new QuoteID(group.getQuoteEntryID().getValue()));
                    }
                }
                System.out.println(" >> sending order = \n" + order);
                send(order);
            }
        } catch (Exception aException) {
            aException.printStackTrace();
        }
    }

    @Override
    public void onMessage(ExecutionReport aExecutionReport,
                          SessionID aSessionID) throws FieldNotFound {
        if (aExecutionReport.getOrdStatus().getValue() == OrdStatus.REJECTED) {
            System.out.println("<< REJECT EXE RPT = " + aExecutionReport);
        } else {
            System.out.println("<< Execution Report = " + aExecutionReport);
        }
    }

    @Override
    public void onMessage(PositionReport aPositionReport,
                          SessionID aSessionID) {
        System.out.println("<< Position Report = " + aPositionReport);
    }

    @Override
    public void onMessage(RequestForPositionsAck aRequestForPositionsAck,
                          SessionID aSessionID) {
        System.out.println("<< Request For Positions Ack = " + aRequestForPositionsAck);
    }

    @Override
    public void onMessage(TradingSessionStatus aSessionStatus,
                          SessionID aSessionID) {
        mSessionStatus = aSessionStatus;
        System.out.println("<< Trading Station Status = " + aSessionStatus);
        getAccounts();
        getOrders();
        getOpenPositions();
        getClosedPositions();
        sendMarketDataRequest(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES);
    }

    @Override
    public void onMessage(MarketDataRequestReject aReject,
                          SessionID aSessionID) throws FieldNotFound {
        System.out.println("<< MarketDataRequestReject = " + aReject);
    }

    @Override
    public void onMessage(SecurityStatus aSecurityStatus,
                          SessionID aSessionID) throws FieldNotFound {
        System.out.println("<< SecurityStatus = " + aSecurityStatus);
    }

    @Override
    public void onMessage(Logout aLogout, SessionID aSessionID) {
        System.out.println("got logout = " + aLogout);
    }

    public void send(Message aMessage) {
        try {
            Session.sendToTarget(aMessage, mSessionID);
        } catch (Exception aException) {
            aException.printStackTrace();
        }
    }

    public void sendEntryOrder() {
        try {
            sendEntryOrder(mCollateralReport, mSessionID);
        } catch (FieldNotFound aFieldNotFound) {
            aFieldNotFound.printStackTrace();
        }
    }

    private void sendEntryOrder(CollateralReport aCollateralReport,
                                SessionID aSessionID) throws FieldNotFound {
        NewOrderSingle order = new NewOrderSingle(new ClOrdID(aSessionID + "-" + System.currentTimeMillis() + "-" + Long.toString(nextID())), new Side(Side.BUY), new TransactTime(), new OrdType(OrdType.LIMIT));
        System.out.println("-- creating order with account = " + aCollateralReport.getAccount());
        order.set(aCollateralReport.getAccount());
        order.set(new Symbol(mInstrument));
        order.set(new OrderQty(mCollateralReport.get(new Quantity()).getValue()));
        order.set(new Price(1.2));
        order.set(new TimeInForce(TimeInForce.GOOD_TILL_CANCEL));
        order.set(new SecondaryClOrdID("fix multi session test"));
        System.out.println(" >> sending order = " + order);
        send(order);
    }

    public void sendMarketDataRequest(char aSubscriptionRequestType) {
        try {
            SubscriptionRequestType subReqType = new SubscriptionRequestType(aSubscriptionRequestType);
            MarketDataRequest mdr = new MarketDataRequest();
            mdr.set(new MDReqID(String.valueOf(nextID())));
            mdr.set(subReqType);
            mdr.set(new MarketDepth(1)); //Top of Book is only choice
            mdr.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));

            MarketDataRequest.NoMDEntryTypes types = new MarketDataRequest.NoMDEntryTypes();
            types.set(new MDEntryType(MDEntryType.BID));
            mdr.addGroup(types);

            types = new MarketDataRequest.NoMDEntryTypes();
            types.set(new MDEntryType(MDEntryType.OFFER));
            mdr.addGroup(types);

            types = new MarketDataRequest.NoMDEntryTypes();
            types.set(new MDEntryType(MDEntryType.TRADING_SESSION_HIGH_PRICE));
            mdr.addGroup(types);

            types = new MarketDataRequest.NoMDEntryTypes();
            types.set(new MDEntryType(MDEntryType.TRADING_SESSION_LOW_PRICE));
            mdr.addGroup(types);

            MarketDataRequest.NoRelatedSym symbol = new MarketDataRequest.NoRelatedSym();
            symbol.set(new Symbol("EUR/USD"));
            mdr.addGroup(symbol);
            /*
            symbol = new MarketDataRequest.NoRelatedSym();
            symbol.set(new Symbol("EUR/JPY"));
            mdr.addGroup(symbol);
            int max = mSessionStatus.getField(new IntField(NoRelatedSym.FIELD)).getValue();
            for (int i = 1; i <= max; i++) {
                SecurityList.NoRelatedSym relatedSym = new SecurityList.NoRelatedSym();
                SecurityList.NoRelatedSym group = (SecurityList.NoRelatedSym) mSessionStatus.getGroup(i,
                                                                                                      relatedSym);
                MarketDataRequest.NoRelatedSym symbol = new MarketDataRequest.NoRelatedSym();
                symbol.set(group.getInstrument());
                mdr.addGroup(symbol);
                SecurityStatusReqID id = new SecurityStatusReqID(String.valueOf(nextID()));
                SecurityStatusRequest ssr = new SecurityStatusRequest(id, subReqType);
                ssr.set(group.getInstrument());
                send(ssr);
            }
            */
            send(mdr);
        } catch (Exception aException) {
            aException.printStackTrace();
        }
    }

    public void sendMassOrderStatusRequest() {
        try {
            OrderMassStatusRequest oh = new OrderMassStatusRequest();
            oh.set(new MassStatusReqID(mSessionID + "-" + System.currentTimeMillis() + "-" + Long.toString(nextID())));
            oh.set(new MassStatusReqType(MassStatusReqType.STATUS_FOR_ALL_ORDERS));
            oh.set(mCollateralReport.getAccount());
            send(oh);
        } catch (Exception aFieldNotFound) {
            aFieldNotFound.printStackTrace();
        }
    }

    public void sendOrder() {
        try {
            sendOrder(mCollateralReport, mSessionID);
        } catch (FieldNotFound aFieldNotFound) {
            aFieldNotFound.printStackTrace();
        }
    }

    private void sendOrder(CollateralReport aCollateralReport,
                           SessionID aSessionID) throws FieldNotFound {
        mClOrdID = new ClOrdID(aSessionID + "-" + System.currentTimeMillis() + "-" + Long.toString(nextID()));
        NewOrderSingle order = new NewOrderSingle(mClOrdID, new Side(Side.BUY), new TransactTime(), new OrdType(OrdType.MARKET));
        System.out.println("-- creating order with account = " + aCollateralReport.getAccount());
        order.set(aCollateralReport.getAccount());
        order.set(new Symbol(mInstrument));
        order.set(new OrderQty(mCollateralReport.get(new Quantity()).getValue()));
        order.set(new TimeInForce(TimeInForce.IMMEDIATE_OR_CANCEL));
        order.set(new SecondaryClOrdID("fix multi session test"));
        System.out.println(" >> sending order = " + order);
        send(order);
    }

    public void sendOrderList() {
        try {
            sendOrderList(mCollateralReport, mSessionID);
        } catch (FieldNotFound aFieldNotFound) {
            aFieldNotFound.printStackTrace();
        }
    }

    private void sendOrderList(CollateralReport aCollateralReport,
                               SessionID aSessionID) throws FieldNotFound {
        NewOrderList list = new NewOrderList();
        list.set(new ListID("testlistid"));
        list.set(new BidType(BidType.NO_BIDDING_PROCESS));
        int max = 2;
        for (int i = 0; i < max; i++) {
            NewOrderList.NoOrders order = new NewOrderList.NoOrders();
            System.out.println("-- creating order with account = " + aCollateralReport.getAccount());
            order.set(new ClOrdID(aSessionID + "-" + System.currentTimeMillis() + "-" + Long.toString(nextID())));
            order.set(new ListSeqNo(i));
            order.set(new Side(Side.BUY));
            order.set(new OrderQty(aCollateralReport.get(new Quantity()).getValue()));
            order.set(new TransactTime());
            order.set(new OrdType(OrdType.MARKET));
            order.set(new Symbol(mInstrument));
            order.set(aCollateralReport.getAccount());
            order.addGroup(aCollateralReport.getGroup(1, new CollateralReport.NoPartyIDs()));
            order.set(new TimeInForce(TimeInForce.IMMEDIATE_OR_CANCEL));
            order.set(new SecondaryClOrdID("fix multi session test"));
            list.addGroup(order);
        }
        list.set(new TotNoOrders(max));
        System.out.println(" >> sending order list = " + list);
        send(list);
    }

    public void sendOrderStatusRequest() {
        try {
            OrderStatusRequest osr = new OrderStatusRequest();
            osr.set(mCollateralReport.getAccount());
            osr.set(new OrderID("13800433"));
            send(osr);
        } catch (Exception aFieldNotFound) {
            aFieldNotFound.printStackTrace();
        }
    }

    public void sendRFP() {
        RequestForPositions rfp = new RequestForPositions();
        rfp.set(new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES));
        rfp.set(new PosReqType(PosReqType.POSITIONS));
        rfp.set(new Account("ALL"));
        rfp.set(new TransactTime(new Date()));
        rfp.set(new AccountType(AccountType.ACCOUNT_IS_CARRIED_ON_NON_CUSTOMER_SIDE_OF_BOOKS_AND_IS_CROSS_MARGINED));
        rfp.set(new PosReqID(String.valueOf(nextID())));
        rfp.set(new ClearingBusinessDate(getDate()));
        send(rfp);
    }

    public void sendResendRequest() {
        ResendRequest rr = new ResendRequest(new BeginSeqNo(1), new EndSeqNo(0));
        System.out.println("sending resend request rr = " + rr);
        send(rr);
    }

    public void sendTestRequest() {
        TestRequest req = new TestRequest();
        req.set(new TestReqID(String.valueOf(nextID())));
        send(req);
    }

    public void sendUserRequest() {
        UserRequest ur = new UserRequest();
        ur.setString(UserRequestID.FIELD, String.valueOf(nextID()));
        ur.setString(Username.FIELD, mUsername);
        ur.setString(Password.FIELD, mPassword);
        ur.setInt(UserRequestType.FIELD, 5);
        System.out.println(">> Sending User Request " + ur);
        send(ur);
    }

    public void setOTO() {
        mOTO = true;
    }

    public void setOpenRange() {
        mOpenRange = true;
    }

    public void setPreviouslyQuoted() {
        mPreviouslyQuoted = true;
    }

    @Override
    public void toAdmin(Message aMessage, SessionID aSessionID) {
    }

    @Override
    public void toApp(Message aMessage, SessionID aSessionID) {
    }
}
