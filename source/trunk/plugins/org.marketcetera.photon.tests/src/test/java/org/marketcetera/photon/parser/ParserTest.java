package org.marketcetera.photon.parser;

import java.math.BigDecimal;

import jfun.parsec.ParserException;
import junit.framework.Test;

import org.marketcetera.core.ClassVersion;
import org.marketcetera.core.ExpectedTestFailure;
import org.marketcetera.core.FIXVersionTestSuite;
import org.marketcetera.core.FIXVersionedTestCase;
import org.marketcetera.core.InMemoryIDFactory;
import org.marketcetera.core.NoMoreIDsException;
import org.marketcetera.photon.commands.CancelCommand;
import org.marketcetera.photon.commands.MessageCommand;
import org.marketcetera.quickfix.FIXVersion;

import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.Account;
import quickfix.field.BeginSeqNo;
import quickfix.field.EndSeqNo;
import quickfix.field.MsgType;
import quickfix.field.OrderQty;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;

@ClassVersion("$Id$")
public class ParserTest extends FIXVersionedTestCase {

	public ParserTest(String inName, FIXVersion version) {
		super(inName, version);
	}

    public static Test suite()
    {
    	FIXVersionTestSuite suite = new FIXVersionTestSuite(ParserTest.class,FIXVersion.values());
        return suite;
    }

    
    public void testNewOrder() throws NoMoreIDsException, FieldNotFound {
    	CommandParser aParser = new CommandParser();
    	aParser.setIDFactory(new InMemoryIDFactory(10));
    	aParser.setMessageFactory(FIXVersion.FIX42.getMessageFactory());
    	String order;  
    	order = "B 100 IBM 1.";
    	MessageCommand command = aParser.parseNewOrder(order);
    	Message result = command.getMessage();
    	verifyNewOrder(result, Side.BUY, new BigDecimal("100"), "IBM", new BigDecimal("1"), TimeInForce.DAY, null);

    	order = "SS 1234 IBM 1.8";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.SELL_SHORT, new BigDecimal("1234"), "IBM", new BigDecimal("1.8"), TimeInForce.DAY, null);

    	order = "ss 1234 IBM 1.8 day";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.SELL_SHORT, new BigDecimal("1234"), "IBM", new BigDecimal("1.8"), TimeInForce.DAY, null);

    	order = "SSE 999 IBM .7";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.SELL_SHORT_EXEMPT, new BigDecimal("999"), "IBM", new BigDecimal(".7"), TimeInForce.DAY, null);

    	order = "S 0 IBM 0.0";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.SELL, new BigDecimal("0"), "IBM", new BigDecimal("0.0"), TimeInForce.DAY, null);

    	order = "B 100 IBM 94.8\tDAY";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.BUY, new BigDecimal("100"), "IBM", new BigDecimal("94.8"), TimeInForce.DAY, null);

    	order = "B 100 IBM 94.8 OPG";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.BUY, new BigDecimal("100"), "IBM", new BigDecimal("94.8"), TimeInForce.AT_THE_OPENING, null);

    	order = "B 100 IBM 94.8 CLO";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.BUY, new BigDecimal("100"), "IBM", new BigDecimal("94.8"), TimeInForce.AT_THE_CLOSE, null);

    	order = "B 100 IBM 94.8 FOK";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.BUY, new BigDecimal("100"), "IBM", new BigDecimal("94.8"), TimeInForce.FILL_OR_KILL, null);

    	order = "B 100 IBM 94.8 IOC";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.BUY, new BigDecimal("100"), "IBM", new BigDecimal("94.8"), TimeInForce.IMMEDIATE_OR_CANCEL, null);

    	order = "B 100 IBM 94.8 GTC";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.BUY, new BigDecimal("100"), "IBM", new BigDecimal("94.8"), TimeInForce.GOOD_TILL_CANCEL, null);

    	order = "SS 100 IBM 94.8 DAY 123.45";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.SELL_SHORT, new BigDecimal("100"), "IBM", new BigDecimal("94.8"), TimeInForce.DAY, "123.45");
    	
    	order = "B 100 IBM 94.8 DAY AAA;A/a-A";
    	command = aParser.parseNewOrder(order);
    	result = command.getMessage();
    	verifyNewOrder(result, Side.BUY, new BigDecimal("100"), "IBM", new BigDecimal("94.8"), TimeInForce.DAY, "AAA;A/a-A");

    	(new ExpectedTestFailure(ParserException.class) {
            protected void execute() throws Throwable
            {
            	String innerOrder = "A 100 IBM 94.8 DAY AAA?A/a-A";
            	CommandParser innerParser = new CommandParser();
            	innerParser.setIDFactory(new InMemoryIDFactory(10));
            	innerParser.setMessageFactory(msgFactory);
            	innerParser.setDataDictionary(fixDD.getDictionary());
            	innerParser.parseNewOrder(innerOrder);
            }
        }).run();

    	(new ExpectedTestFailure(ParserException.class) {
            protected void execute() throws Throwable
            {
            	String innerOrder = "SS A IBM 94.8 DAY AAA?A/a-A";
            	CommandParser innerParser = new CommandParser();
            	innerParser.setIDFactory(new InMemoryIDFactory(10));
            	innerParser.setMessageFactory(msgFactory);
            	innerParser.setDataDictionary(fixDD.getDictionary());
            	innerParser.parseNewOrder(innerOrder);
            }
        }).run();

    	(new ExpectedTestFailure(ParserException.class) {
            protected void execute() throws Throwable
            {
            	String innerOrder = "SS 100 IBM XXX DAY AAA?A/a-A";
            	CommandParser innerParser = new CommandParser();
            	innerParser.setIDFactory(new InMemoryIDFactory(10));
            	innerParser.setMessageFactory(msgFactory);
            	innerParser.setDataDictionary(fixDD.getDictionary());
            	innerParser.parseNewOrder(innerOrder);
            }
        }).run();

    	(new ExpectedTestFailure(ParserException.class) {
            protected void execute() throws Throwable
            {
            	String innerOrder = "SS 100 IBM 123.45 ASDF AAA?A/a-A";
            	CommandParser innerParser = new CommandParser();
            	innerParser.setIDFactory(new InMemoryIDFactory(10));
            	innerParser.setMessageFactory(msgFactory);
            	innerParser.setDataDictionary(fixDD.getDictionary());
            	innerParser.parseNewOrder(innerOrder);
            }
        }).run();

    	(new ExpectedTestFailure(ParserException.class) {
            protected void execute() throws Throwable
            {
            	String innerOrder = "SS 100 IBM ";
            	CommandParser innerParser = new CommandParser();
            	innerParser.setIDFactory(new InMemoryIDFactory(10));
            	innerParser.setMessageFactory(msgFactory);
            	innerParser.setDataDictionary(fixDD.getDictionary());
            	innerParser.parseNewOrder(innerOrder);
            }
        }).run();

    	(new ExpectedTestFailure(ParserException.class) {
            protected void execute() throws Throwable
            {
            	String innerOrder = "s 15 toli mkt bob errtok";
            	CommandParser innerParser = new CommandParser();
            	innerParser.setIDFactory(new InMemoryIDFactory(10));
            	innerParser.setMessageFactory(msgFactory);
            	innerParser.setDataDictionary(fixDD.getDictionary());
            	MessageCommand aCommand = innerParser.parseNewOrder(innerOrder);
            	System.out.println(""+aCommand);
            }
        }).run();
    }
    
    void verifyNewOrder(Message message, char side, BigDecimal quantity,
    		String symbol, BigDecimal price, char timeInForce, String account) throws FieldNotFound
    {
    	assertEquals(side, message.getChar(Side.FIELD));
    	assertEquals(quantity, new BigDecimal(message.getString(OrderQty.FIELD)));
    	assertEquals(symbol, message.getString(Symbol.FIELD));
    	assertEquals(price, new BigDecimal(message.getString(Price.FIELD)));
    	assertEquals(timeInForce, message.getChar(TimeInForce.FIELD));
    	try {
    		assertEquals(account, message.getString(Account.FIELD));
    	} catch (FieldNotFound ex) {
    		assertNull(account);
    	}
    }
    
    public void testResendRequest() throws FieldNotFound {
    	CommandParser aParser = new CommandParser();
    	aParser.setIDFactory(new InMemoryIDFactory(10));
    	aParser.setMessageFactory(FIXVersion.FIX42.getMessageFactory());
    	String commandText;  
    	MessageCommand command;
    	Message rr;

    	commandText = "RR 0 0";
		command = (MessageCommand) aParser.parseCommand(commandText);
		rr = command.getMessage();
    	assertEquals(MsgType.RESEND_REQUEST, rr.getHeader().getString(MsgType.FIELD));
    	assertEquals(0, rr.getInt(BeginSeqNo.FIELD));
    	assertEquals(0, rr.getInt(EndSeqNo.FIELD));

    	commandText = "RR 0 9";
		command = (MessageCommand) aParser.parseCommand(commandText);
		rr = command.getMessage();
    	assertEquals(MsgType.RESEND_REQUEST, rr.getHeader().getString(MsgType.FIELD));
    	assertEquals(0, rr.getInt(BeginSeqNo.FIELD));
    	assertEquals(9, rr.getInt(EndSeqNo.FIELD));

    	commandText = "RR 9 0";
		command = (MessageCommand) aParser.parseCommand(commandText);
		rr = command.getMessage();
    	assertEquals(MsgType.RESEND_REQUEST, rr.getHeader().getString(MsgType.FIELD));
    	assertEquals(9, rr.getInt(BeginSeqNo.FIELD));
    	assertEquals(0, rr.getInt(EndSeqNo.FIELD));

    	commandText = "RR 24 38";
		command = (MessageCommand) aParser.parseCommand(commandText);
		rr = command.getMessage();
    	assertEquals(MsgType.RESEND_REQUEST, rr.getHeader().getString(MsgType.FIELD));
    	assertEquals(24, rr.getInt(BeginSeqNo.FIELD));
    	assertEquals(38, rr.getInt(EndSeqNo.FIELD));
    }
    
    public void testFullOrder() throws FieldNotFound{
    	CommandParser aParser = new CommandParser();
    	aParser.setIDFactory(new InMemoryIDFactory(10));
    	aParser.setMessageFactory(msgFactory);
    	aParser.setDataDictionary(fixDD.getDictionary());
    	
    	String order;
    	order = "O B 100 IBM 1.";
    	MessageCommand command = (MessageCommand) aParser.parseCommand(order);
    	Message result = command.getMessage();
    	verifyNewOrder(result, Side.BUY, new BigDecimal("100"), "IBM", new BigDecimal("1"), TimeInForce.DAY, null);
    	
    }

    public void testCancelOrder() throws NoMoreIDsException, FieldNotFound{
    	CommandParser aParser = new CommandParser();
    	aParser.setIDFactory(new InMemoryIDFactory(10));
    	aParser.setMessageFactory(msgFactory);
    	aParser.setDataDictionary(fixDD.getDictionary());

    	String command;

    	command = "C 12345";
    	CancelCommand comm = (CancelCommand)aParser.parseCommand(command);
    	assertEquals("12345", comm.getID());
    }
    
    public void testDecimalQuantity() throws Exception {
    	if (ParserTest.this.fixVersion==FIXVersion.FIX40 || 
    			ParserTest.this.fixVersion==FIXVersion.FIX41){
	    	(new ExpectedTestFailure(ParserException.class) {
	            protected void execute() throws Throwable
	            {
	        		doTestDecimalQuantity();
	            }
	        }).run();
    	} else {
    		doTestDecimalQuantity();
    	}
	}

    private void doTestDecimalQuantity() {
    	String innerOrder = "SS 100.1 IBM 123.4";
    	CommandParser innerParser = new CommandParser();
    	innerParser.setIDFactory(new InMemoryIDFactory(10));
    	innerParser.setMessageFactory(msgFactory);
    	innerParser.setDataDictionary(fixDD.getDictionary());
    	innerParser.parseNewOrder(innerOrder);
    }
    
//    public void testCancelAll() throws ParserException, NoMoreIDsException, FieldNotFound
//    {
//    	CommandParser aParser = new CommandParser();
//    	aParser.setIDFactory(new InMemoryIDFactory(10));
//
//    	String command;
//
//    	command = "CA";
//    	Message aMessage = aParser.parseNewOrder(command).getMessage();
//    	assertEquals(MsgType.ORDER_CANCEL_REQUEST, aMessage.getHeader().getString(MsgType.FIELD));
//    }
   

   
}

