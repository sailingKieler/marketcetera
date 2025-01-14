package org.marketcetera.trade;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.marketcetera.core.IDFactory;
import org.marketcetera.core.InMemoryIDFactory;
import org.marketcetera.core.NoMoreIDsException;
import org.marketcetera.event.HasFIXMessage;
import org.marketcetera.quickfix.FIXDataDictionary;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.quickfix.SystemFIXMessageFactory;
import org.marketcetera.util.log.I18NBoundMessage1P;
import org.marketcetera.util.log.I18NBoundMessage2P;
import org.marketcetera.util.misc.ClassVersion;

import quickfix.Field;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.StringField;
import quickfix.field.BeginString;

/* $License$ */
/**
 * Factory for creating various messages.
 *
 * @author anshul@marketcetera.com
 * @version $Id$
 * @since 1.0.0
 */
@ClassVersion("$Id$")
class FactoryImpl
        extends Factory
{
    @Override
    public OrderSingle createOrderSingle() {
        OrderSingleImpl order = new OrderSingleImpl();
        assignOrderID(order);
        return order;
    }

    @Override
    public OrderSingleSuggestion createOrderSingleSuggestion() {
        return new OrderSingleSuggestionImpl();
    }

    @Override
    public OrderCancel createOrderCancel(ExecutionReport inLatestReport) {
        OrderCancelImpl order = new OrderCancelImpl();
        assignOrderID(order);
        if (inLatestReport != null) {
            order.setAccount(inLatestReport.getAccount());
            order.setBrokerID(inLatestReport.getBrokerId());
            order.setBrokerOrderID(inLatestReport.getBrokerOrderID());
            order.setQuantity(inLatestReport.getOrderQuantity());
            order.setSide(inLatestReport.getSide());
            order.setInstrument(inLatestReport.getInstrument());
            order.setText(inLatestReport.getText());
            addCustomFieldsFromReport(inLatestReport, order, SystemFIXMessageFactory.EXECUTION_REPORT_FIELDS);
            Map<String,String> customFields = order.getCustomFields();
            if(customFields != null) {
                customFields.remove(String.valueOf(quickfix.field.MaxFloor.FIELD));
                order.setCustomFields(customFields);
            }
            // set this manually after the customFields are copied, so that we take the OrigClOrdId from the report itself,
            // not from the custom field of the report
            order.setOriginalOrderID(inLatestReport.getOrderID());
        }
        return order;
    }

    @Override
    public OrderReplace createOrderReplace(ExecutionReport inLatestReport) {
        OrderReplaceImpl order = new OrderReplaceImpl();
        assignOrderID(order);
        if (inLatestReport != null) {
            order.setAccount(inLatestReport.getAccount());
            order.setBrokerID(inLatestReport.getBrokerId());
            order.setBrokerOrderID(inLatestReport.getBrokerOrderID());
            order.setOrderType(inLatestReport.getOrderType());
            order.setPrice(inLatestReport.getPrice());
            order.setQuantity(inLatestReport.getOrderQuantity());
            order.setDisplayQuantity(inLatestReport.getOrderDisplayQuantity());
            order.setSide(inLatestReport.getSide());
            order.setInstrument(inLatestReport.getInstrument());
            order.setTimeInForce(inLatestReport.getTimeInForce());
            order.setOrderCapacity(inLatestReport.getOrderCapacity());
            order.setPositionEffect(inLatestReport.getPositionEffect());
            order.setText(inLatestReport.getText());
            addCustomFieldsFromReport(inLatestReport, order, SystemFIXMessageFactory.CANCEL_REPLACE_EXCLUSION_FIELDS);

            // set this manually after the customFields are copied, so that we take the OrigClOrdId from the report itself,
            // not from the custom field of the report 
            order.setOriginalOrderID(inLatestReport.getOrderID());
        }
        return order;
    }

    /** Extracts the FIX message from the incoming ER, pulls the custom fields out of it, and sets it on the specified order */
    protected void addCustomFieldsFromReport(ExecutionReport inLatestReport, RelatedOrder order, Set<Integer> inExcludeFields) {
        if(inLatestReport instanceof HasFIXMessage) {
            Message msg = ((HasFIXMessage) inLatestReport).getMessage();
            try {
                order.setCustomFields(getFieldMap(msg, inExcludeFields));
            } catch (MessageCreationException e) {
                // ignore: unable to insert custom fields
            }
        }
    }

    @Override
    public FIXOrder createOrder(Message inMessage, BrokerID inBrokerID)
            throws MessageCreationException {
        assignOrderID(inMessage);
        return new FIXOrderImpl(inMessage, inBrokerID);
    }

    @Override
    public ExecutionReport createExecutionReport(Message inMessage,
                                                 BrokerID inBrokerID,
                                                 Originator inOriginator,
                                                 UserID inActorID,
                                                 UserID inViewerID)
            throws MessageCreationException {
        if(inMessage == null) {
            throw new NullPointerException();
        }
        if(inOriginator == null) {
            throw new NullPointerException();
        }
        if(FIXMessageUtil.isExecutionReport(inMessage)) {
            return new ExecutionReportImpl(inMessage,
                    inBrokerID, inOriginator, inActorID, inViewerID);
        } else {
            throw new MessageCreationException(new I18NBoundMessage1P(
                    Messages.NOT_EXECUTION_REPORT, inMessage.toString()));
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.Factory#createExecutionReport(quickfix.Message, org.marketcetera.trade.BrokerID, org.marketcetera.trade.Originator, org.marketcetera.trade.Hierarchy, org.marketcetera.trade.UserID, org.marketcetera.trade.UserID)
     */
    @Override
    public ExecutionReport createExecutionReport(Message inMessage,
                                                 BrokerID inBrokerID,
                                                 Originator inOriginator,
                                                 Hierarchy inHierarchy,
                                                 UserID inActorID,
                                                 UserID inViewerID)
            throws MessageCreationException
    {
        if(inMessage == null) {
            throw new NullPointerException();
        }
        if(inOriginator == null) {
            throw new NullPointerException();
        }
        if(inHierarchy == null) {
            throw new NullPointerException();
        }
        if(FIXMessageUtil.isExecutionReport(inMessage)) {
            return new ExecutionReportImpl(inMessage,
                                           inBrokerID,
                                           inOriginator,
                                           inHierarchy,
                                           inActorID,
                                           inViewerID);
        } else {
            throw new MessageCreationException(new I18NBoundMessage1P(
                    Messages.NOT_EXECUTION_REPORT, inMessage.toString()));
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.Factory#createOrderCancelReject(quickfix.Message, org.marketcetera.trade.BrokerID, org.marketcetera.trade.Originator, org.marketcetera.trade.Hierarchy, org.marketcetera.trade.UserID, org.marketcetera.trade.UserID)
     */
    @Override
    public OrderCancelReject createOrderCancelReject(Message inMessage,
                                                     BrokerID inBrokerID,
                                                     Originator inOriginator,
                                                     Hierarchy inHierarchy,
                                                     UserID inActorID,
                                                     UserID inViewerID)
            throws MessageCreationException
    {
        if(inMessage == null) {
            throw new NullPointerException();
        }
        if(inOriginator == null) {
            throw new NullPointerException();
        }
        if(inHierarchy == null) {
            throw new NullPointerException();
        }
        if(FIXMessageUtil.isCancelReject(inMessage)) {
            return new OrderCancelRejectImpl(inMessage,
                                             inBrokerID,
                                             inOriginator,
                                             inHierarchy,
                                             inActorID,
                                             inViewerID);
        } else {
            throw new MessageCreationException(new I18NBoundMessage1P(Messages.NOT_CANCEL_REJECT,
                                                                      inMessage.toString()));
        }
    }
    @Override
    public OrderCancelReject createOrderCancelReject(Message inMessage,
                                                     BrokerID inBrokerID,
                                                     Originator inOriginator,
                                                     UserID inActorID,
                                                     UserID inViewerID)
            throws MessageCreationException
    {
        if(inMessage == null) {
            throw new NullPointerException();
        }
        if(inOriginator == null) {
            throw new NullPointerException();
        }
        if(FIXMessageUtil.isCancelReject(inMessage)) {
            return new OrderCancelRejectImpl(
                    inMessage, inBrokerID, inOriginator, inActorID, inViewerID);
        } else {
            throw new MessageCreationException(new I18NBoundMessage1P(
                    Messages.NOT_CANCEL_REJECT, inMessage.toString()));
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.Factory#createFIXResponse(quickfix.Message, org.marketcetera.trade.BrokerID, org.marketcetera.trade.Originator, org.marketcetera.trade.Hierarchy, org.marketcetera.trade.UserID, org.marketcetera.trade.UserID)
     */
    @Override
    public FIXResponse createFIXResponse(Message inMessage,
                                         BrokerID inBrokerID,
                                         Originator inOriginator,
                                         Hierarchy inHierarchy,
                                         UserID inActorID,
                                         UserID inViewerID)
    {
        if(inMessage == null) {
            throw new NullPointerException();
        }
        if(inOriginator == null) {
            throw new NullPointerException();
        }
        if(inHierarchy == null) {
            throw new NullPointerException();
        }
        return new FIXResponseImpl(inMessage,
                                   inBrokerID,
                                   inOriginator,
                                   inHierarchy,
                                   inActorID,
                                   inViewerID);
    }
    @Override
    public FIXResponse createFIXResponse(Message inMessage,
                                         BrokerID inBrokerID,
                                         Originator inOriginator,
                                         UserID inActorID,
                                         UserID inViewerID)
    {
        if (inMessage==null) {
            throw new NullPointerException();
        }
        if (inOriginator==null) {
            throw new NullPointerException();
        }
        return new FIXResponseImpl(inMessage, inBrokerID, inOriginator,
                                   inActorID, inViewerID);
    }

    @Override
    public OrderSingle createOrderSingle(Message inMessage, 
                                         BrokerID inBrokerID)
            throws MessageCreationException {
        checkSystemMessage(inMessage);
        if(!FIXMessageUtil.isOrderSingle(inMessage)) {
            throw new MessageCreationException(new I18NBoundMessage1P(
                    Messages.NOT_SINGLE_ORDER, inMessage.toString()));
        }
        OrderSingle order = createOrderSingle();
        order.setAccount(FIXUtil.getAccount(inMessage));
        order.setExecutionDestination(FIXUtil.getExecutionDestination(inMessage));
        order.setBrokerID(inBrokerID);
        order.setCustomFields(getFieldMap(inMessage, SystemFIXMessageFactory.ORDER_SINGLE_FIELDS));
        order.setOrderID(FIXUtil.getOrderID(inMessage));
        order.setOrderType(FIXUtil.getOrderType(inMessage));
        order.setPrice(FIXUtil.getPrice(inMessage));
        order.setQuantity(FIXUtil.getOrderQuantity(inMessage));
        order.setDisplayQuantity(FIXUtil.getOrderDisplayQuantity(inMessage));
        order.setSide(FIXUtil.getSide(inMessage));
        order.setInstrument(FIXUtil.getInstrument(inMessage));
        order.setTimeInForce(FIXUtil.getTimeInForce(inMessage));
        order.setOrderCapacity(FIXUtil.getOrderCapacity(inMessage));
        order.setPositionEffect(FIXUtil.getPositionEffect(inMessage));
        order.setText(FIXUtil.getText(inMessage));
        assignOrderID(order);
        return order;
    }

    @Override
    public OrderCancel createOrderCancel(Message inMessage,
                                         BrokerID inBrokerID)
            throws MessageCreationException {
        checkSystemMessage(inMessage);
        if(!FIXMessageUtil.isCancelRequest(inMessage)) {
            throw new MessageCreationException(new I18NBoundMessage1P(
                    Messages.NOT_CANCEL_ORDER, inMessage.toString()));
        }
        OrderCancel order = new OrderCancelImpl();
        order.setAccount(FIXUtil.getAccount(inMessage));
        order.setBrokerID(inBrokerID);
        order.setBrokerOrderID(FIXUtil.getBrokerOrderID(inMessage));
        order.setCustomFields(getFieldMap(inMessage, SystemFIXMessageFactory.ORDER_CANCEL_FIELDS));
        order.setOrderID(FIXUtil.getOrderID(inMessage));
        order.setOriginalOrderID(FIXUtil.getOriginalOrderID(inMessage));
        order.setQuantity(FIXUtil.getOrderQuantity(inMessage));
        order.setSide(FIXUtil.getSide(inMessage));
        order.setInstrument(FIXUtil.getInstrument(inMessage));
        order.setText(FIXUtil.getText(inMessage));
        assignOrderID(order);
        return order;
    }

    @Override
    public OrderReplace createOrderReplace(
            Message inMessage,
            BrokerID inBrokerID)
            throws MessageCreationException {
        checkSystemMessage(inMessage);
        if(!FIXMessageUtil.isCancelReplaceRequest(inMessage)) {
            throw new MessageCreationException(new I18NBoundMessage1P(
                    Messages.NOT_CANCEL_REPLACE_ORDER, inMessage.toString()));
        }
        OrderReplace order = new OrderReplaceImpl();
        order.setAccount(FIXUtil.getAccount(inMessage));
        order.setExecutionDestination(FIXUtil.getExecutionDestination(inMessage));
        order.setBrokerID(inBrokerID);
        order.setBrokerOrderID(FIXUtil.getBrokerOrderID(inMessage));
        order.setCustomFields(getFieldMap(inMessage, SystemFIXMessageFactory.ORDER_REPLACE_FIELDS));
        order.setOrderID(FIXUtil.getOrderID(inMessage));
        order.setOrderType(FIXUtil.getOrderType(inMessage));
        order.setOriginalOrderID(FIXUtil.getOriginalOrderID(inMessage));
        order.setPrice(FIXUtil.getPrice(inMessage));
        order.setQuantity(FIXUtil.getOrderQuantity(inMessage));
        order.setDisplayQuantity(FIXUtil.getOrderDisplayQuantity(inMessage));
        order.setSide(FIXUtil.getSide(inMessage));
        order.setInstrument(FIXUtil.getInstrument(inMessage));
        order.setTimeInForce(FIXUtil.getTimeInForce(inMessage));
        order.setOrderCapacity(FIXUtil.getOrderCapacity(inMessage));
        order.setPositionEffect(FIXUtil.getPositionEffect(inMessage));
        order.setText(FIXUtil.getText(inMessage));
        assignOrderID(order);
        return order;
    }

    @Override
    public void setOrderIDFactory(IDFactory inIDFactory) {
        if(inIDFactory == null) {
            throw new NullPointerException();
        }
        mIDFactory = inIDFactory;
    }

    /**
     * Assigns a unique order ID to the supplied order.
     *
     * @param inOrder the order that needs to be assigned a unique order ID.
     */
    private void assignOrderID(OrderBase inOrder) {
        inOrder.setOrderID(new OrderID(getNextOrderID()));
    }

    /**
     * Assigns a unique order ID to the supplied order.
     *
     * @param inMessage the order message that needs to be
     * assigned a unique order ID.
     */
    private void assignOrderID(Message inMessage) {
        FIXUtil.setOrderID(inMessage, getNextOrderID());
    }

    /**
     * Fetches the next orderID value from the ID factory.
     *
     * @return the next orderID value.
     *
     * @throws IllegalArgumentException if the ID factory was not
     * configured correctly.
     */
    private String getNextOrderID() throws IllegalArgumentException {
        try {
            return mIDFactory.getNext();
        } catch (NoMoreIDsException e) {
            //Indicates that id factories are not correctly assembled
            //to prevent failure. The factory should not throw exceptions.
            //In case it's unable to generate IDs, it should rely on a
            //local, in-memory ID generator that cannot fail
            Messages.UNABLE_TO_GENERATE_IDS.error(this, e);
            throw new IllegalArgumentException(
                    Messages.UNABLE_TO_GENERATE_IDS.getText(), e);
        }
    }
    private volatile IDFactory mIDFactory = new InMemoryIDFactory(System.nanoTime());

    /**
     * Returns all the fields contained in the supplied message as a map.
     * The map has the field tag number as the key and the field string
     * value as the value.
     * <p>
     * The returned map excludes the set of fields supplied in
     * <code>inExcludeFields</code>.
     * <p>
     * Byte Fields are currently not supported. If the supplied message
     * includes any byte fields, an exception will be thrown.
     *
     * @param inMessage The message whose fields need to be converted into
     * the map.
     * @param inExcludeFields The message fields that should not be included
     * in the returned map.
     *
     * @return the map containing fields of the supplied message.
     *
     * @throws MessageCreationException if there were errors.
     */
    static Map<String,String> getFieldMap(Message inMessage,
                                          Set<Integer> inExcludeFields)
            throws MessageCreationException {
        Map<String,String> fields = new HashMap<String, String>();
        Iterator<Field<?>> iterator = inMessage.iterator();
        while(iterator.hasNext()) {
            Field<?> f = iterator.next();
            if(inExcludeFields != null && inExcludeFields.contains(f.getTag())) {
                continue;
            }
            //all fix fields except the one's that contain binary data
            //are stored as string fields. All of them can be safely
            //retrieved as string fields.
            if(f instanceof StringField) {
                fields.put(String.valueOf(f.getTag()),
                        ((StringField)f).getObject());
            }
            //ignore fields that contain binary data.
        }
        return fields.isEmpty()
                ? null
                : fields;
    }

    /**
     * Verify if the supplied message is a system FIX message, ie. it's
     * not a regular FIX message. And that it does not contain any groups.
     *
     * @param inMessage the message to be verified. Cannot be null.
     *
     * @throws MessageCreationException if the message fails any of the checks.
     */
    private void checkSystemMessage(Message inMessage)
            throws MessageCreationException {
        if(inMessage == null) {
            throw new NullPointerException();
        }
        try {
            //Verify that the message is a System FIX Message, not a
            //regular FIX Message.
            String beginString = inMessage.getHeader().getField(
                    new BeginString()).getValue();
            if(!FIXDataDictionary.FIX_SYSTEM_BEGIN_STRING.equals(beginString)) {
                throw new MessageCreationException(new I18NBoundMessage1P(
                        Messages.NON_SYSTEM_FIX_MESSAGE,beginString));
            }
        } catch (FieldNotFound inFieldNotFound) {
            throw new MessageCreationException(inFieldNotFound,
                    new I18NBoundMessage1P(
                            Messages.SYSTEM_FIX_MESSAGE_NO_BEGIN_STRING,
                            inMessage.toString()));
        }
        //Verify that the message does not contain any groups as
        //we do not support messages with groups.
        Iterator<Integer> iterator = inMessage.groupKeyIterator();
        if(iterator.hasNext()) {
            throw new MessageCreationException(new I18NBoundMessage2P(
                    Messages.MESSAGE_HAS_GROUPS, iterator.next(),
                    inMessage.toString()));
        }
    }
}
